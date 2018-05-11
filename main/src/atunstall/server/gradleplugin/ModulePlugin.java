package atunstall.server.gradleplugin;

import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.PluginManager;
import org.gradle.plugins.ide.idea.IdeaPlugin;

public class ModulePlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        applyPlugins(target.getPluginManager());
        applyExtensions(target, target.getExtensions());
        target.afterEvaluate(IDEAFix::applyFix);
    }

    private void applyPlugins(PluginManager pm) {
        pm.apply(JavaPlugin.class);
        pm.apply(IdeaPlugin.class);
    }

    private void applyExtensions(Project project, ExtensionContainer ec) {
        JavaPluginConvention jpc = project.getConvention().getPlugin(JavaPluginConvention.class);
        ec.create("component", ComponentExtension.class, project, jpc);
        jpc.setSourceCompatibility(JavaVersion.VERSION_1_9);
        jpc.setTargetCompatibility(JavaVersion.VERSION_1_9);
    }
}
