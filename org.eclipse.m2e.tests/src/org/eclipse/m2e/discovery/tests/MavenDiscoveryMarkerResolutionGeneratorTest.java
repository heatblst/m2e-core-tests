package org.eclipse.m2e.discovery.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.internal.lifecycle.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecycle.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.internal.discovery.markers.MavenDiscoveryMarkerResolutionGenerator;
import org.eclipse.m2e.tests.common.AbstractLifecycleMappingTest;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;
import org.junit.Test;


public class MavenDiscoveryMarkerResolutionGeneratorTest extends AbstractLifecycleMappingTest {

  IMarkerResolutionGenerator2 generator;

  public void setUp() throws Exception {
    super.setUp();
    generator = new MavenDiscoveryMarkerResolutionGenerator();
  }

  @Test
  public void testCanResolveConfiguratorMarker() throws Exception {
    LifecycleMappingMetadataSource defaultMetadata = loadLifecycleMappingMetadataSource("projects/discovery/defaultMetadata.xml");
    LifecycleMappingFactory.setDefaultLifecycleMappingMetadataSource(defaultMetadata);

    IMavenProjectFacade facade = importMavenProject("projects/discovery", "configurator/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();

    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertEquals("Error markers", 2, errorMarkers.size());

    WorkspaceHelpers
        .assertConfiguratorErrorMarkerAttributes(errorMarkers.get(0),
            "no such project configurator id for test-lifecyclemapping-plugin:test-goal-for-eclipse-extension2 - embedded from pom");
    assertTrue("Resolve configurator marker", generator.hasResolutions(errorMarkers.get(0)));
    IMarkerResolution[] resolutions = generator.getResolutions(errorMarkers.get(0));
    assertEquals(1, resolutions.length);

  }

  @Test
  public void testCanResolvePackagingMarker() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/discovery", "unknownPackaging/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();

    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertEquals("Error markers", 1, errorMarkers.size());

    WorkspaceHelpers.assertLifecyclePackagingErrorMarkerAttributes(errorMarkers.get(0), "rar");
    assertTrue("Resolve packaging marker", generator.hasResolutions(errorMarkers.get(0)));
  }

  @Test
  public void testCanResolveLifecycleIdMarker() throws Exception {
    LifecycleMappingMetadataSource defaultMetadata = loadLifecycleMappingMetadataSource("projects/discovery/defaultMetadata.xml");
    LifecycleMappingFactory.setDefaultLifecycleMappingMetadataSource(defaultMetadata);

    IMavenProjectFacade facade = importMavenProject("projects/discovery", "lifecycleId/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();

    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertEquals("Error markers", 2, errorMarkers.size());

    WorkspaceHelpers.assertLifecycleIdErrorMarkerAttributes(errorMarkers.get(0), "unknown-or-missing");
    assertTrue("Resolve packaging marker", generator.hasResolutions(errorMarkers.get(0)));
  }

  @Test
  public void testCanResolveMojoExecutionMarker() throws Exception {
    IMavenProjectFacade facade = importMavenProject("projects/discovery", "mojoExecutions/pom.xml");
    assertNotNull("Expected not null MavenProjectFacade", facade);
    IProject project = facade.getProject();

    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    assertEquals("Error markers", 2, errorMarkers.size());

    ILifecycleMapping lifecycleMapping = projectConfigurationManager.getLifecycleMapping(facade, monitor);
    List<MojoExecutionKey> notCoveredMojoExecutions = lifecycleMapping.getNotCoveredMojoExecutions(monitor);

    WorkspaceHelpers.assertErrorMarkerAttributes(errorMarkers.get(0), notCoveredMojoExecutions.get(0));
    assertTrue("Resolve MojoExecution marker", generator.hasResolutions(errorMarkers.get(0)));
  }

  @Test
  public void testResolveMultipleMarkers() throws Exception {
    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject[] projects = importProjects("projects/discovery", new String[] {"configurator/pom.xml", "unknownPackaging/pom.xml",
        "lifecycleId/pom.xml", "mojoExecutions/pom.xml"}, configuration);
    waitForJobsToComplete();

    List<IMarker> errorMarkers = new ArrayList<IMarker>();
    for(IProject project : projects) {
      IMavenProjectFacade facade = mavenProjectManager.create(project, monitor);
      IProject p = facade.getProject();
      errorMarkers.addAll(WorkspaceHelpers.findErrorMarkers(p));
    }

    IMarkerResolution m = generator.getResolutions(errorMarkers.get(0))[0];

    assertTrue(m instanceof WorkbenchMarkerResolution);

    IMarker[] resolvable = ((WorkbenchMarkerResolution) m).findOtherMarkers(errorMarkers
        .toArray(new IMarker[errorMarkers.size()]));
    // One fewer marker than the total otherwise the marker used to generate the resolution will be shown twice
    assertEquals(6, resolvable.length);
    assertFalse(Arrays.asList(resolvable).contains(errorMarkers.get(0)));
  }
}
