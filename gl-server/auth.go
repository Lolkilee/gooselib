package main

import (
	"time"

	"github.com/boltdb/bolt"
	"github.com/gofiber/fiber/v2"
	"github.com/gofiber/fiber/v2/log"
)

// Helper function to make authenticating requests easy
func authenticate(c *fiber.Ctx, db *bolt.DB) bool {
	user_inp := c.Get("user")
	pass := c.Get("pass") // plain text password (TLS handles encryption)

	user, err := db_get[User](db, DB_USERS, user_inp)

	if err != nil {
		log.Warn(err)
		return false
	}

	if user.Pass == pass {
		user.LastLogin = time.Now()
		if err := db_set[User](db, DB_USERS, user.Name, user); err != nil {
			log.Warn(err)
			return false
		}
		return true
	}
	return false
}

func create_user_ep(c *fiber.Ctx) error {
	user_inp := c.Get("user")
	pass := c.Get("pass")

	res := FailableResponse{
		Success: true,
	}

	if !db_contains_key(glob_db, DB_USERS, user_inp) {
		create_user(glob_db, user_inp, pass)
		return c.JSON(res)
	}

	res.Success = false
	res.Reason = "username already exists in database"
	return c.JSON(res)
}

func remove_user_ep(c *fiber.Ctx) error {
	res := FailableResponse{
		Success: false,
		Reason:  "invalid password",
	}

	if !db_contains_key(glob_db, DB_USERS, c.Get("user")) {
		res.Reason = "user does not exist"
		return c.JSON(res)
	}

	if authenticate(c, glob_db) {
		remove_user(glob_db, c.Get("user"))
		res.Success = true
		res.Reason = ""
	}

	return c.JSON(res)
}

func create_user(db *bolt.DB, name string, pass string) {
	usr := User{Name: name, Pass: pass}
	if err := db_set[User](db, DB_USERS, name, usr); err != nil {
		log.Warn(err)
	}
}

func remove_user(db *bolt.DB, name string) {
	err := db_delete(db, DB_USERS, name)
	if err != nil {
		log.Warn(err)
	}
}
