package main

import (
	"gooselib/shared"
	"net"
	"sync"
)

var (
	mu     sync.Mutex
	listen net.Listener
)

func InitSvr(PORT string, TYPE string) {
	shared.Log(shared.INFO, "starting server on port "+PORT)
	var err error
	listen, err = net.Listen(TYPE, ":"+PORT)
	if err != nil {
		shared.Err(err)
	}
}

func CloseSvr() {
	mu.Lock()
	defer mu.Unlock()

	if listen != nil {
		shared.ILog("closing server listener")
		listen.Close()
		listen = nil
	}
}

func HandleReqs(conn net.Conn) {
	//TODO
}

func RunSvr() {
	shared.ILog("starting server listen loop")
	for {
		mu.Lock()
		if listen == nil {
			mu.Unlock()
			break
		}
		mu.Unlock()

		conn, err := listen.Accept()
		if err != nil {
			mu.Lock()
			if listen == nil {
				mu.Unlock()
				break
			}
			mu.Unlock()
			shared.Err(err)
			continue
		}
		go HandleReqs(conn)
	}
}
