# DbUnit test files.

## Files in this directory

The `dbunit-*.dtd` files are generated from an Ant task.  (Currently,
`dbunit-registry-export-choice.dtd` is updated by the `testng` and
`testng-bamboo` tasks. The other DTDs are not generated directly by a
build, but can be updated as needed using the generate-DTD task.

The `blank-*.xml` files are created/edited by hand.

## The `tests` directory

The `tests` directory has subdirectories with names based on the names
of the test methods that use their contents.
