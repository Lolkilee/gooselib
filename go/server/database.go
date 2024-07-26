package main

import (
	"gooselib/shared"

	badger "github.com/dgraph-io/badger/v4"
)

const DB_PATH = "./db.bin"

var db *badger.DB

func InitDB(temp bool) {
	if !temp {
		shared.ILog("starting internal database")
	} else {
		shared.ILog("starting internal database in memory mode")
	}

	var err error
	db, err = badger.Open(badger.DefaultOptions(DB_PATH).WithInMemory(temp).WithLogger(nil))
	if err != nil {
		shared.Err(err)
	}
}

func CloseDB() {
	shared.ILog("closing database")
	if db != nil {
		db.Close()
		db = nil
	}
}

func UserExists(username string) bool {
	err := db.View(func(txn *badger.Txn) error {
		_, err := txn.Get([]byte(username))
		return err
	})

	return err == nil
}

func GetUser(username string) (*User, error) {
	var u *User
	err := db.View(func(txn *badger.Txn) error {
		item, err := txn.Get([]byte(username))
		if err != nil {
			return err
		}

		var bytes []byte
		err = item.Value(func(val []byte) error {
			bytes = append([]byte{}, val...)
			return nil
		})

		if err == nil {
			u, err = BytesToUser(bytes)
		}

		return err
	})

	return u, err
}

func PutUser(username, password string) {
	if UserExists(username) {
		shared.Log(shared.WARNING, "overwriting existing user '"+username+"'")
	}

	u := NewUser(username, password)
	err := db.Update(func(txn *badger.Txn) error {
		arr, err := UserToBytes(u)
		if err != nil {
			shared.Err(err)
			return err
		}
		return txn.Set([]byte(username), arr)
	})

	if err != nil {
		shared.Err(err)
	}

	shared.Log(shared.DEBUG, "put username '"+username+"' in database")
}

func AuthUser(username, password string) bool {
	if !UserExists(username) {
		return false
	}

	u, err := GetUser(username)
	if err != nil {
		shared.Log(shared.ERROR, "error during authentication")
		shared.Err(err)
		return false
	}

	return ComparePass(password, u.Hash)
}
