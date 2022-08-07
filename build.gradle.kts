import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.text.SimpleDateFormat
import java.util.*

plugins {
	id("build-dashboard")// See build/reports/buildDashboard/index.html
	id("com.dorongold.task-tree") version "2.1.0"
	id("org.owasp.dependencycheck") version "7.1.1"// To push security to the left
	id("com.github.ben-manes.versions") version "0.42.0"
	id("io.gitlab.arturbosch.detekt") version "1.21.0"

	id("org.springframework.boot") version "2.7.2"
	id("io.spring.dependency-management") version "1.0.12.RELEASE"
	kotlin("jvm") version "1.7.10"
	kotlin("plugin.spring") version "1.7.10"
	id("org.jetbrains.kotlin.plugin.jpa") version "1.7.10" apply false
}

// TODO: probably, makes sense to create an empty root project and add build settings to subprojects...
allprojects {
	group = "com.stringconcat"
	version = "0.0.1"

	repositories {
		mavenCentral()
		jcenter()
		maven { url = uri("https://repo.spring.io/milestone") }
		maven { url = uri("https://repo.spring.io/snapshot") }
	}

	val failOnWarning = project.properties["allWarningsAsErrors"] != null && project
		.properties["allWarningsAsErrors"] == "true"

	tasks.withType<KotlinCompile> {
		kotlinOptions {
			freeCompilerArgs = listOf("-Xjsr305=strict")
			jvmTarget = "1.8"
			allWarningsAsErrors = failOnWarning
		}
	}

	configurations.all {
		resolutionStrategy {
			eachDependency {
				requested.version?.contains("snapshot", true)?.let {
					if (it) {
						throw GradleException("Snapshot found: ${requested.name} ${requested.version}")
					}
				}
			}
		}
	}

	apply {
		plugin("base")
		plugin("io.gitlab.arturbosch.detekt")
		plugin("org.owasp.dependencycheck")
		plugin("com.github.ben-manes.versions")
	}

	detekt {
		repositories {
			mavenCentral()
		}

		config = files("${rootProject.projectDir}/tools/detekt/detekt-config.yml")
		buildUponDefaultConfig = true
		input = files("src/main/kotlin", "src/test/kotlin", "src/test/gatling")

		reports {
			html.enabled = true
		}

		dependencies {
			detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.21.0")
		}
	}

	tasks {

		val check = named<DefaultTask>("check")
		val dependencyUpdate =
			named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates")

		dependencyUpdate {
			revision = "release"
			outputFormatter = "txt"
			checkForGradleUpdate = true
			outputDir = "$buildDir/reports/dependencies"
			reportfileName = "updates"
		}

		dependencyUpdate.configure {

			fun isNonStable(version: String): Boolean {
				val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
				val regex = "^[0-9,.v-]+(-r)?$".toRegex()
				val isStable = stableKeyword || regex.matches(version)
				return isStable.not()
			}

			rejectVersionIf {
				isNonStable(candidate.version) && !isNonStable(currentVersion)
			}
		}

		dependencyCheck {
			failBuildOnCVSS = 2f
			cveValidForHours = 3
			suppressionFile = "${rootProject.projectDir}/tools/dependency-check/dependency-check-suppression.xml"
		}

		check {
			// It is important to understand that the first time this task is executed it may take 5-20 minutes
			// as it downloads and processes the data from the National Vulnerability Database (NVD)
			// After the first batch download, as long as the plugin is executed at least once every seven days
			// the update will only take a few seconds.
			finalizedBy(dependencyCheckAnalyze)
			finalizedBy(dependencyUpdate)
		}

		withType<Test> {
			useJUnitPlatform()

			testLogging {
				events(
					org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
					org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
					org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
				)
				showStandardStreams = true
				exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
			}

			systemProperties["pact.rootDir"] = "${rootProject.buildDir}/pacts"
		}

	}
}

java.sourceCompatibility = JavaVersion.VERSION_1_8

dependencies {
	// spring modules
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-rest")

	// kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	implementation(project(":presentation"))
	implementation(project(":persistence"))
	implementation(project(":useCasePeople"))
	implementation(project(":businessPeople"))
	implementation(project(":quoteGarden"))
	implementation(project(":avatarsDicebear"))

	// dev tools
	developmentOnly("org.springframework.boot:spring-boot-devtools:2.7.2")

	//persistance
	implementation("org.postgresql:postgresql:42.4.1")
	implementation("org.liquibase:liquibase-core:4.15.0")

	// tests
//	testCompile("org.junit.jupiter:junit-jupiter-api:5.8.2")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	testImplementation("io.projectreactor:reactor-test:3.4.21")
}

val gitCommitHash: String by lazy {
	if (project.hasProperty("buildSHA")) {
		project.property("buildSHA").toString()
	} else "no git info"
}

val currentTime: String by lazy {
	SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SZ").run {
		timeZone = TimeZone.getTimeZone("UTC")
		format(Date())
	}
}

tasks {

	register<Copy>("installLocalGitHook") {
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		from(layout.projectDirectory.file("tools/git-hook/pre-push"))
		into(layout.projectDirectory.dir(".git/hooks"))
		fileMode = Integer.parseUnsignedInt("755", 8)
	}

	register("setup_banner") {
		doFirst {
			file("src/main/resources/le0nx-banner.txt").writeText(
				"""
					
				/**************************************************/
					BUILD SHA: $gitCommitHash
					VERSION: $version
					TIME: $currentTime
				/**************************************************/
				
				""".trimIndent()
			)
		}
	}

	bootRun {
		dependsOn(":installLocalGitHook")
		dependsOn(":setup_banner")
		mustRunAfter(":setup_banner")
		if (project.hasProperty("args")) {
			args(project.property("args").toString().split(","))
		}
	}

	bootJar {
		dependsOn(":installLocalGitHook")
		dependsOn(":setup_banner")
		mustRunAfter(":setup_banner")
	}

}