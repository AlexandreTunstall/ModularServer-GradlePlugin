package atunstall.server.gradleplugin

import groovy.xml.MarkupBuilder
import org.gradle.api.Project

import java.util.stream.Stream

class IDEAFix {
    static void applyFix(Project p) {
        p.getRootProject().getTasks().maybeCreate('setupEnv').dependsOn("$p.path:idea")
        MarkupBuilder xml = new MarkupBuilder(new FileWriter(p.rootProject.file(".idea/runConfigurations/${p.name}.xml")))
        String modulePath = walkDependencyTree(p).map {it.sourceSets.main.output}.reduce(p.sourceSets.main.output, { l, n -> l += n}, { a, b -> [a, b]}).asPath
        xml.component(name: 'ProjectRunConfigurationManager') {
            configuration(default: 'false', name: p.name, type: 'Application', factoryName: 'Application', singleton: 'true') {
                extension(name: 'COVERAGE', enabled: 'false', merge: 'false', sample_coverage: 'true', runner: 'idea')
                option(name: 'MAIN_CLASS_NAME', value: 'atunstall.server.core.impl.Start')
                option(name: 'VM_PARAMETERS', value: "-p \"$modulePath\" --add-modules $p.component.id")
                option(name: 'PROGRAM_PARAMETERS', value: '')
                option(name: 'WORKING_DIRECTORY', value: 'file://$PROJECT_DIR$/run')
                module(name: p.name)
            }
        }
    }

    private static Stream<Project> walkDependencyTree(Project p) {
        Deque<Project> queue = new ArrayDeque<>()
        Set<Project> result = new HashSet<>()
        queue.add(p)
        while (queue.size() > 0) {
            queue.pop().component.needed.forEach {
                Project p2 = p.project(":$it")
                queue.add(p2)
                result.add(p2)
            }
        }
        return result.stream()
    }
}
