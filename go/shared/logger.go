package shared

import (
	"fmt"
	"strings"
)

const (
	ERROR   int = 0
	WARNING     = 1
	INFO        = 2
	DEBUG       = 3
)

var curLevel int = DEBUG

func Log(lvl int, msg string) {
	if lvl <= curLevel {
		var header string = "[" + LvlToString(lvl) + "] "
		fmt.Println(header + msg)
	}
}

func ILog(msg string) {
	Log(INFO, msg)
}

func Err(err error) {
	Log(ERROR, err.Error())
}

func LvlToString(lvl int) string {
	switch lvl {
	case ERROR:
		return "ERROR"
	case WARNING:
		return "WARNING"
	case INFO:
		return "INFO"
	case DEBUG:
		return "DEBUG"
	default:
		return "UNKNOWN"
	}
}

func StringToLvl(lvl string) int {
	switch strings.ToUpper(lvl) {
	case "ERROR":
		return ERROR
	case "WARNING":
		return WARNING
	case "INFO":
		return INFO
	case "DEBUG":
		return DEBUG
	default:
		return ERROR
	}
}
