package shared

import (
	"bytes"
	"encoding/binary"
)

const (
	LATEST_PKT_VER = 1
)

type GLPacket struct {
	Version uint8
	Type    uint8
	Length  uint16
	Data    []byte
}

func (p *GLPacket) Serialize() ([]byte, error) {
	buf := new(bytes.Buffer)
	err := binary.Write(buf, binary.LittleEndian, p.Version)
	err = binary.Write(buf, binary.LittleEndian, p.Type)
	err = binary.Write(buf, binary.LittleEndian, p.Length)
	err = binary.Write(buf, binary.LittleEndian, p.Data)

	if err != nil {
		return nil, err
	}

	return buf.Bytes(), nil
}

func (p *GLPacket) Deserialize(data []byte) error {
	buf := bytes.NewReader(data)
	err := binary.Read(buf, binary.LittleEndian, &p.Version)
	err = binary.Read(buf, binary.LittleEndian, &p.Type)
	err = binary.Read(buf, binary.LittleEndian, &p.Length)
	err = binary.Read(buf, binary.LittleEndian, &p.Data)

	if err != nil {
		return err
	}

	return nil
}

func NewPacket(pType uint8, len uint16, data []byte) *GLPacket {
	p := GLPacket{Version: LATEST_PKT_VER}
	p.Type = pType
	p.Length = len
	p.Data = data

	return &p
}
