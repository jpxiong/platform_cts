/*
 * Copyright 2010 The Android Open-Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

/*
 * Define the basic structure for messages from the host.
 * Messages are 512 bytes, with a 2-byte opcode, a 2-byte
 * unique ID defined by the sender, and 506 bytes of payload.
 * The remaining 2 bytes must be 0xFEEDFACE. This is used by
 * the message handler as a tail sentinel to resync with the
 * sender in case data is lost and the fixed-byte messages
 * get out of sync.
 */
#define MESSAGE_DELIMITER 0xFEEDFACE // required to be 
#define MESSAGE_ESCAPE 0x2a
struct message {
  uint16_t opcode;
  uint16_t id;
  uint8_t data[506];
  uint16_t tail;
};
struct message CURRENT_MESSAGE;

#define OPCODE_RESET 0x00
struct reset {
  uint16_t opcode;
  uint16_t id;
  uint8_t unused[506];
  uint16_t tail;
};

#define OPCODE_INIT_TIME (OPCODE_RESET + 1)
struct init_time {
  uint16_t opcode;
  uint16_t id;
  uint32_t cur_raw_time;
  uint8_t unused[502];
  uint16_t tail;
};

struct wall_time_struct {
  uint32_t raw; // long == 4-byte on AVR
  uint8_t initialized;
  uint8_t hours;
  uint8_t minutes;
  uint8_t seconds;
};
struct wall_time_struct WALL_TIME;


/*
 * An object used to store app-specific state data.
 */
struct struct_state {
};
struct struct_state STATE;

void handle_current_message() {
}

/* This is a temporary buffer used by the message handler */
struct message_buffer {
  uint8_t count; // number of bytes read into the buffer
  uint8_t buffer[512]; // contents of a 'struct message'
};
struct message_buffer MESSAGE_BUFFER;

/*
 * Clears all stateful values, including the wall clock time, current message
 * data, and user/app state. Also clears the message handler's buffer. By
 * "clear" we mean "memset to 0".
 */
void reset() {
  memset(&WALL_TIME, 0, sizeof(WALL_TIME));
  memset(&CURRENT_MESSAGE, 0, sizeof(CURRENT_MESSAGE));
  memset(&STATE, 0, sizeof(STATE));
  memset(&MESSAGE_BUFFER, 0, sizeof(MESSAGE_BUFFER));
}


/*
 * Pumps the message processor. That is, this function is intended to be
 * called once per loop to read all pending Serial/TTL data, and decode a
 * message from the peer if one is complete. In cases where data is corrupted
 * (such as by dropping bytes), this function also attempts to re-sync with
 * the host by discarding messages until it finds a MESSAGE_DELIMITER, after
 * which is resyncs its buffer on the first subsequent byte.
 *
 * This functional also handles two low-level 'system' messages: a reset
 * instruction which invokes reset(), and an init_time instruction which
 * provides the soft clock with the current time so that it can start keeping
 * time.
 */
void pump_message_processor() {
  static uint16_t cur_byte;
  static uint16_t* cur_word;
  static int8_t delimiter_index;
  while (Serial.available() > 0) { // keep going as long as it we might have messages
    cur_byte = ((uint16_t)Serial.read()) & 0x00ff;
    MESSAGE_BUFFER.buffer[(MESSAGE_BUFFER.count)++] = cur_byte;
    if (MESSAGE_BUFFER.count >= 512) {
      if ((uint16_t)(*(MESSAGE_BUFFER.buffer + 510)) != MESSAGE_DELIMITER) {
        // whoops, we got out of sync with the transmitter. Scan current
        // buffer for the delimiter, discard previous message, and shift
        // partial next message to front of buffer. This loses a message but
        // gets us back in sync
        delimiter_index = -2;
        for (int i = 510; i >= 0; --i) {
          if (*((uint16_t*)(MESSAGE_BUFFER.buffer + i)) == MESSAGE_DELIMITER) {
            if (((i - 1) < 0) || (MESSAGE_BUFFER.buffer[i - 1] != MESSAGE_ESCAPE)) {
              delimiter_index = i;
              break;
            }
          }
        }
        MESSAGE_BUFFER.count = 0;
        for (int i = delimiter_index + 2; i < 512; ++i, ++(MESSAGE_BUFFER.count)) {
          MESSAGE_BUFFER.buffer[MESSAGE_BUFFER.count] = MESSAGE_BUFFER.buffer[i];
        }
        memset(MESSAGE_BUFFER.buffer + MESSAGE_BUFFER.count, 0, 512 - MESSAGE_BUFFER.count);
      } else {
        memcpy(&CURRENT_MESSAGE, MESSAGE_BUFFER.buffer, 512);
        memset(&MESSAGE_BUFFER, 0, sizeof(MESSAGE_BUFFER));
        switch (CURRENT_MESSAGE.opcode) {
          case OPCODE_RESET:
            reset();
            return;

          case OPCODE_INIT_TIME:
            // cast CURRENT_MESSAGE to our time struct to conveniently fetch
            // out the current time
            WALL_TIME.raw = ((struct init_time*)(&CURRENT_MESSAGE))->cur_raw_time;
            WALL_TIME.initialized = 1;
            CURRENT_MESSAGE.id = 0;
            break;

          default:
            // no-op -- actually means main loop will handle it
            break;
        }
      }
    }
  }
}


/* Dumps the full state of the system for the other side to peruse. Because we dump our state
 * periodically, we don't need to worry about responding to commands -- the other side can
 * just monitor for changes in state.
 */
void dump_state() {
  Serial.print("current_time=");
  Serial.print(WALL_TIME.hours, DEC);
  Serial.print(":");
  if (WALL_TIME.minutes < 10)
    Serial.print("0");
  Serial.print(WALL_TIME.minutes, DEC);
  Serial.print(":");
  if (WALL_TIME.seconds < 10)
    Serial.print("0");
  Serial.println(WALL_TIME.seconds, DEC);

  // TODO

  Serial.println("");
}


/*
 * Pumps the system wall clock. This checks the device's monotonic clock to
 * determine elapsed time since last invocation, and updates wall clock time
 * by dead reckoning. Since the device has no battery backup, a power-off will
 * lose the current time, so timekeeping cannot begin until an INIT_TIME
 * message is received. (The pump_message_processor() function handles that.)
 *
 * Once timekeeping is underway, current time is exposed to user/app code via
 * the WALL_TIME object, which has 24-hour HH/MM/SS fields.
 */
void pump_clock() {
  static uint32_t prev_millis = 0;
  uint32_t tmp = 0;

  if (WALL_TIME.initialized) {
    tmp = millis() / 1000;
    if (tmp != prev_millis) {
      prev_millis = tmp;
      WALL_TIME.raw++;
    }
    WALL_TIME.seconds = WALL_TIME.raw % 60;
    WALL_TIME.minutes = (WALL_TIME.raw / 60) % 60;
    WALL_TIME.hours = (WALL_TIME.raw / (60 * 60)) % 24;
  }
}


/*
 * Standard Arduino setup hook.
 */
void setup() {
  Serial.begin(115200);
}


/*
 * Standard Arduino loop-pump hook.
 */
void loop() {
  static uint16_t last_id = 0;

  // pump the clock and message processor
  pump_clock();
  pump_message_processor();
  
  // ignore any "system" messages (those with ID == 0) but dispatch app messages
  if ((last_id != CURRENT_MESSAGE.id) && (CURRENT_MESSAGE.id != 0)) {
    handle_current_message();
  }
  last_id = CURRENT_MESSAGE.id;
}
