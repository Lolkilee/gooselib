package main

import (
	"flag"
	"gooselib/shared"
	"os"
	"strconv"
	"syscall"

	"github.com/ztrue/shutdown"
)

const (
	PORT = "61234"
	TYPE = "tcp"
)

var adminPass string

func ShutdownHook(sig os.Signal) {
	shared.Log(shared.WARNING, "shutdown hook called due to signal: "+sig.String())
	CloseDB()
	CloseSvr()
}

func ReadArguments() bool {
	flag.StringVar(&adminPass, "pass", "admin", "admin password")
	var tempFlag = flag.Bool("np", false, "set database persistence mode")

	return *tempFlag
}

func main() {
	shutdown.AddWithParam(ShutdownHook)

	InitDB(ReadArguments())
	PutUser("admin", adminPass)
	shared.Log(shared.DEBUG, "testing admin pass, result: "+strconv.FormatBool(AuthUser("admin", adminPass)))
	defer CloseDB()

	InitSvr(PORT, TYPE)
	defer CloseSvr()

	go shutdown.Listen(syscall.SIGINT, syscall.SIGTERM)
	RunSvr()
}
