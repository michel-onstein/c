/*

Copyright 2020 Q-Jam B.V.

*/
package package_manager

type PackageManager interface {
	// Files needed by the manager
	FilesNeeded() []string

	// Get the packages installed according to the supplied files
	Get(files []string) []Package
}

type Package struct {
	Name string
	Version string
}