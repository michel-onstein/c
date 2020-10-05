/*

Copyright 2020 Q-Jam B.V.

*/
package package_manager

type PackageManager interface {
	// Files needed by the manager
	FilesNeeded() []string
}

type Package struct {
	Name string
	Version string
}