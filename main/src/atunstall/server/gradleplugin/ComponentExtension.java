package atunstall.server.gradleplugin;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ComponentExtension {
    @Internal
    private final Project project;
    @Internal
    private final JavaPluginConvention jpc;

    @Internal
    private String id;
    @Internal
    private String name;
    @Internal
    private boolean ap;
    @Internal
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")       // Field is read by needed
    private List<String> needed;

    public ComponentExtension(Project project, JavaPluginConvention jpc) {
        this.project = project;
        this.jpc = jpc;
        Configuration api = project.getConfigurations().maybeCreate("api");
        //api.extendsFrom(project.getConfigurations().findByName(jpc.getSourceSets().getByName("main").getApiConfigurationName()));
        project.getDependencies().add("api", jpc.getSourceSets().getByName("main").getOutput());
        api.extendsFrom(project.getConfigurations().getByName("implementation"));
        project.getArtifacts().add("api", project.getTasks().getByName(jpc.getSourceSets().getByName("main").getJarTaskName()));
        Configuration apc = project.getConfigurations().maybeCreate("ap");
        apc.extendsFrom(project.getConfigurations().findByName("compileOnly"));
        project.getTasks().withType(JavaCompile.class).forEach(jc -> jc.doFirst(t -> jc.getOptions().getCompilerArgs()
                .addAll(List.of("--module-path", jc.getClasspath().getAsPath()))));
        jpc.getSourceSets().forEach(this::configureSourceSet);
        needed = new ArrayList<>();
    }

    public void setId(String id) {
        this.id = id;
        project.setGroup(id);
        project.getTasks().withType(JavaCompile.class).forEach(t -> t.getInputs().property("moduleName", id));
    }

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
        project.getTasks().withType(Jar.class).forEach(t -> t.setAppendix(name));
    }

    public String getName() {
        return name;
    }

    public void setAp(boolean ap) {
        if (this.ap || !ap) return;
        this.ap = true;
        SourceSet ss = jpc.getSourceSets().create("ap", this::configureSourceSet);
        project.getDependencies().add("compileOnly", ss.getOutput());
        project.getTasks().maybeCreate("compileApJava", JavaCompile.class).getInputs().property("moduleName", project.getGroup());
    }

    public boolean getAp() {
        return ap;
    }

    public void setNeeded(Iterable<String> needed) {
        needed.forEach(this::needed);
    }

    public List<String> getNeeded() {
        return needed;
    }

    private SourceSet configureSourceSet(SourceSet ss) {
        ss.getJava().setSrcDirs(List.of(ss.getName() + "/src"));
        ss.getResources().setSrcDirs(List.of(ss.getName() + "/rsc"));
        return ss;
    }

    private void needed(String module) {
        DependencyHandler dh = project.getDependencies();
        Project dep = project.evaluationDependsOn(":" + module);
        dep.getPluginManager().apply(ModulePlugin.class);
        dh.add("implementation", dh.project(Map.of("path", ":" + module, "configuration", "api")));
        dh.add("compileOnly", dh.project(Map.of("path", ":" + module, "configuration", "ap")));
        needed.add(module);
    }
}
