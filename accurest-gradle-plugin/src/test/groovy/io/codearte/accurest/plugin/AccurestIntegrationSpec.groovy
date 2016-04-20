package io.codearte.accurest.plugin

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

import static java.nio.charset.StandardCharsets.UTF_8

/**
 * @author Olga Maciaszek-Sharma
 * @author Denis Stepanov
 * @since 23.02.16
 */
abstract class AccurestIntegrationSpec extends Specification {

	File testProjectDir

	def setup() {
		def dateString = new Date().format("yyyy-MM-dd_HH-mm-ss")
		def testFolder = new File("build/generated-tests/${getClass().simpleName}/${dateString}")
		testFolder.mkdirs()
		testProjectDir = testFolder
	}

	public static final String SPOCK = "targetFramework = 'Spock'"
	public static final String JUNIT = "targetFramework = 'JUnit'"
	public static final String MVC_SPEC = "baseClassForTests = 'com.blogspot.toomuchcoding.MvcSpec'"
	public static final String MVC_TEST = "baseClassForTests = 'com.blogspot.toomuchcoding.MvcTest'"

	protected void setupForProject(String projectRoot) {
		copyResourcesToRoot(projectRoot)

		def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
		if (pluginClasspathResource == null) {
			throw new IllegalStateException("Did not find 'plugin-classpath.txt'")
		}

		String pluginClasspath = pluginClasspathResource.readLines()
				.collect { it.replace('\\', '\\\\') } // escape backslashes in Windows paths
				.collect { "'$it'" }
				.join(", ")

		buildFile << """
			buildscript {
				dependencies {
					classpath files($pluginClasspath)
				}
			}
		"""

		// Extending buildscript is required when 'apply' is used.
		// 'GradleRunner#withPluginClasspath' can be used when plugin is added using 'plugins { id...'
	}

	protected void switchToJunitTestFramework() {
		switchToJunitTestFramework(MVC_SPEC, MVC_TEST)
	}

	protected void switchToJunitTestFramework(String from, String to) {
		Path path = buildFile.toPath()
		String content = new StringBuilder(new String(Files.readAllBytes(path), UTF_8)).replaceAll(SPOCK, JUNIT)
				.replaceAll(from, to)
		Files.write(path, content.getBytes(UTF_8))
	}

	protected void runTasksSuccessfully(String... tasks) {
		BuildResult result = run(tasks)
		result.tasks.each {
			assert it.outcome == TaskOutcome.SUCCESS || it.outcome == TaskOutcome.UP_TO_DATE
		}
	}

	protected BuildResult validateTasksOutcome(BuildResult result, TaskOutcome expectedOutcome, String... tasks) {
		tasks.each {
			BuildTask task = result.task(":" + it)
			assert task
			assert task.outcome == expectedOutcome
		}
		return result
	}

	protected BuildResult run(String... tasks) {
		return GradleRunner.create()
				.withProjectDir(testProjectDir)
				.withArguments(tasks)
				.build()
	}

	protected void copyResourcesToRoot(String srcDir) {
		copyResources(srcDir, testProjectDir)
	}

	protected void copyResources(String srcDir, File destinationFile) {
		ClassLoader classLoader = getClass().getClassLoader()
		URL resource = classLoader.getResource(srcDir)
		if (resource == null) {
			throw new RuntimeException("Could not find classpath resource: $srcDir")
		}
		File resourceFile = new File(resource.toURI())
		if (resourceFile.file) {
			FileUtils.copyFile(resourceFile, destinationFile)
		} else {
			FileUtils.copyDirectory(resourceFile, destinationFile)
		}
	}

	protected File file(String path) {
		return new File(testProjectDir, path)
	}

	protected boolean fileExists(String path) {
		return file(path).exists()
	}

	protected File getBuildFile() {
		return new File('build.gradle', testProjectDir)
	}

}
