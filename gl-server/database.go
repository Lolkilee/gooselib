package main

import (
	"github.com/boltdb/bolt"
	"github.com/gofiber/fiber/v2/log"
)

// Helper functions for the database

var DB_BUCKET_NAMES = [...]string{"users"}

// creates necessary buckets if they dont exist yet
func init_db(db *bolt.DB) {
	log.Info("Initializing buckets in database")
	for i := 0; i < len(DB_BUCKET_NAMES); i++ {
		init_bucket(db, DB_BUCKET_NAMES[i])
	}
}

func init_bucket(db *bolt.DB, name string) {
	db.Update(func(tx *bolt.Tx) error {
		_, err := tx.CreateBucketIfNotExists([]byte(name))
		if err != nil {
			log.Warn("Error creating bucket")
			log.Warn(err)
		}
		return nil
	})
}
