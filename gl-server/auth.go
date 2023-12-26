package main

import (
	"github.com/boltdb/bolt"
	"github.com/gofiber/fiber/v2"
	"github.com/gofiber/fiber/v2/log"
)

// Helper function to make authenticating requests easy
func authenticate(c *fiber.Ctx, db *bolt.DB) bool {
	user_inp := c.Get("user")
	pass := c.Get("pass") // plain text password (TLS handles encryption)

	user, err := db_get[User](db, "users", user_inp)

	// TODO FIX!!!
	if err != nil {
		log.Warn(err)
		return false
	}
	return user.Pass == pass
}

func create_user(db *bolt.DB, name string, pass string) {
	usr := User{Name: name, Pass: pass}
	if err := db_set[User](db, "users", name, usr); err != nil {
		log.Warn(err)
	}
}
