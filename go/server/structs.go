package main

import (
	"bytes"
	"encoding/gob"
	"gooselib/shared"
)

type User struct {
	Username string
	Hash     string
}

func NewUser(username string, password string) *User {
	hash, err := HashPassword(password)
	if err != nil {
		shared.Err(err)
		return nil
	}

	u := User{Username: username, Hash: hash}
	return &u
}

func UserToBytes(u *User) ([]byte, error) {
	var buf bytes.Buffer
	enc := gob.NewEncoder(&buf)
	err := enc.Encode(u)
	if err != nil {
		return nil, err
	}
	return buf.Bytes(), nil
}

func BytesToUser(data []byte) (*User, error) {
	buf := bytes.NewBuffer(data)
	dec := gob.NewDecoder(buf)
	var u User
	err := dec.Decode(&u)
	if err != nil {
		return nil, err
	}
	return &u, nil
}
