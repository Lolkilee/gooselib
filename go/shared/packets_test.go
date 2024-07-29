package shared_test

import (
	"crypto/rand"
	"gooselib/shared"
	"testing"
)

func TestPKTSerialization(t *testing.T) {
	rData := make([]byte, 32)
	rand.Read(rData)
	og := shared.NewPacket(1, 32, rData)

	ser, err := og.Serialize()
	if err != nil {
		t.Errorf("Err during serialization: %v", err)
	}

	deser := &shared.GLPacket{}
	err = deser.Deserialize(ser)
	if err != nil {
		t.Errorf("Err during deserialization: %v", err)
	}
}
