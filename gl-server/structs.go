package main

import "time"

type User struct {
	Name      string
	Pass      string
	LastLogin time.Time
}

type FailableResponse struct {
	Success bool   // True = success, False = failure
	Reason  string // empty when Status is true
}

type App struct {
	Name     string
	Versions []AppVersion
}

type AppVersion struct {
	VersionName string
	Hash        string
	FileSize    int64
}
