# Robust network design

Research prototype for various problem classes in robust network design.

## Setting up
* Clone the repository.
* Required for running the code is a Gurobi installation of version >=9.0.2.
  Point the Gradle build system to the installation location of Gurobi.
  You can either do this by:
  - Setting the `gurobiLibDir` property in a local Gradle properties file (see https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties).
  - Replace the line `val gurobiLibDir: String? by project` with `val gurobiLibDir = "/your/path/to/gurobi/lib"`.
    The path should point to the lib path of the gurobi installation, which contains the `gurobi.jar` file.
* The project uses Gradle as build system and should resolve all other dependencies automatically.
