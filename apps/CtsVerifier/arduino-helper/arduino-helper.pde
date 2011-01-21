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
 * Messages are MESSAGE_SIZE bytes, with a 2-byte opcode, a 2-byte
 * unique ID defined by the sender, and the remainder payload.
 * The remaining 2 bytes must be 0xFEEDFACE. This is used by
 * the message handler as a tail sentinel to resync with the
 * sender in case data is lost and the fixed-byte messages
 * get out of sync.
 */
#define MESSAGE_SIZE 128 // serial buffer size on TTL is 128 bytes
#define MESSAGE_DELIMITER 0xBEEF
#define MESSAGE_ESCAPE 0x2a
struct message {
  uint16_t opcode;
  uint16_t id;
  uint8_t data[MESSAGE_SIZE - 6];
  uint16_t tail;
};
struct message CURRENT_MESSAGE;

#define OPCODE_RESET 0x00
struct reset {
  uint16_t opcode;
  uint16_t id;
  uint8_t unused[MESSAGE_SIZE - 4];
};

#define OPCODE_INIT_TIME (OPCODE_RESET + 1)
struct init_time {
  uint16_t opcode;
  uint16_t id;
  uint32_t cur_raw_time;
  uint8_t unused[MESSAGE_SIZE - 6];
};

#define OPCODE_CURRENT_TIME (OPCODE_RESET + 2)
struct current_time { // we never actually use this, but here for consistency
  uint16_t opcode;
  uint16_t id;
  uint8_t unused[MESSAGE_SIZE - 4];
};

#define OPCODE_SETMODE_PONG (OPCODE_RESET + 3)
struct setmode_pong {
  uint16_t opcode;
  uint16_t id;
  uint16_t playfield_width;
  uint16_t playfield_height;
  uint16_t ball_x;
  uint16_t ball_y;
  uint16_t ball_radius;
  uint16_t ball_velocity_x;
  uint16_t ball_velocity_y;
  uint8_t unused[MESSAGE_SIZE - 18];
};

#define OPCODE_PONG_BALL_STATE (OPCODE_RESET + 4)
struct pong_ball_state {
  uint16_t opcode;
  uint16_t id;
  uint8_t unused[MESSAGE_SIZE - 4];
};

struct wall_time_struct {
  uint32_t raw;
  uint8_t initialized;
  uint8_t hours;
  uint8_t minutes;
  uint8_t seconds;
};
struct wall_time_struct WALL_TIME;

/*
 * Main ball-playing state record.
 */
struct pong_state_struct {
  uint16_t playfield_width;
  uint16_t playfield_height;
  int16_t ball_x;
  int16_t ball_y;
  int16_t ball_radius;
  int16_t velocity_x;
  int16_t velocity_y;
};
struct pong_state_struct PONG_STATE;


/* This is a temporary buffer used by the message handler */
struct message_buffer {
  uint16_t count; // number of bytes read into the buffer
  uint8_t buffer[MESSAGE_SIZE]; // contents of a 'struct message'
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
  memset(&MESSAGE_BUFFER, 0, sizeof(MESSAGE_BUFFER));
  memset(&PONG_STATE, 0, sizeof(PONG_STATE));
}


/*
 * Closes out a serial response, which involves sending a blank line in
 * between messages. Also prints the current time, for funsies.
 */
void close_response() {
  if (WALL_TIME.initialized) {
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
  } else {
    Serial.println("current_time=00:00:00");
  }
  Serial.print("\r\n");
}


/*
 * Opcode processor/handler.
 */
void handle_current_message() {
  static uint16_t last_id = 999999;
  static struct setmode_pong* setmode_pong_msg;

  if (CURRENT_MESSAGE.id == last_id) {
    return;
  }
  last_id = CURRENT_MESSAGE.id;

  switch (CURRENT_MESSAGE.opcode) {

    case OPCODE_SETMODE_PONG: // initialize ball animation state
      memset(&PONG_STATE, 0, sizeof(PONG_STATE));
      setmode_pong_msg = (struct setmode_pong*)(&CURRENT_MESSAGE);
      PONG_STATE.playfield_width = setmode_pong_msg->playfield_width;
      PONG_STATE.playfield_height = setmode_pong_msg->playfield_height;
      PONG_STATE.ball_x = setmode_pong_msg->ball_x;
      PONG_STATE.ball_y = setmode_pong_msg->ball_y;
      PONG_STATE.ball_radius = setmode_pong_msg->ball_radius;
      PONG_STATE.velocity_x = setmode_pong_msg->ball_velocity_x;
      PONG_STATE.velocity_y = setmode_pong_msg->ball_velocity_y;

      Serial.println("message_type=setmode_pong_ack");
      Serial.print("id=");
      Serial.println(CURRENT_MESSAGE.id);
      Serial.print("ball_x=");
      Serial.println(PONG_STATE.ball_x, DEC);
      Serial.print("ball_y=");
      Serial.println(PONG_STATE.ball_y, DEC);
      Serial.print("ball_velocity_x=");
      Serial.println(PONG_STATE.velocity_x, DEC);
      Serial.print("ball_velocity_y=");
      Serial.println(PONG_STATE.velocity_y, DEC);
      Serial.print("playfield_width=");
      Serial.println(PONG_STATE.playfield_width, DEC);
      Serial.print("playfield_height=");
      Serial.println(PONG_STATE.playfield_height, DEC);
      Serial.print("ball_radius=");
      Serial.println(PONG_STATE.ball_radius, DEC);
      close_response();
      break;

    case OPCODE_PONG_BALL_STATE: // update a frame
      /* This gets called once per update/refresh request from host. From the
       * perspective of the AVR, we are running this animation in arbitrary
       * time units. If host calls this at 10 FPS, we return data at 10 FPS.
       * If it calls us at 100FPS, we return data at 100FPS. */
      PONG_STATE.ball_x += PONG_STATE.velocity_x;
      PONG_STATE.ball_y += PONG_STATE.velocity_y;

      // all we do is bounce around the inside of the box we were given
      if ((PONG_STATE.ball_x - PONG_STATE.ball_radius) < 0) {
        PONG_STATE.velocity_x *= -1;
        PONG_STATE.ball_x = PONG_STATE.ball_radius;
      } else if ((PONG_STATE.ball_x + PONG_STATE.ball_radius) > PONG_STATE.playfield_width) {
        PONG_STATE.velocity_x *= -1;
        PONG_STATE.ball_x = PONG_STATE.playfield_width - PONG_STATE.ball_radius;
      }

      if ((PONG_STATE.ball_y - PONG_STATE.ball_radius) < 0) {
        PONG_STATE.velocity_y *= -1;
        PONG_STATE.ball_y = PONG_STATE.ball_radius;
      } else if ((PONG_STATE.ball_y + PONG_STATE.ball_radius) > PONG_STATE.playfield_height) {
        PONG_STATE.velocity_y *= -1;
        PONG_STATE.ball_y = PONG_STATE.playfield_height - PONG_STATE.ball_radius;
      }

      Serial.println("message_type=pong_paddle_state");
      Serial.print("id=");
      Serial.println(CURRENT_MESSAGE.id, DEC);
      Serial.print("ball_x=");
      Serial.println(PONG_STATE.ball_x, DEC);
      Serial.print("ball_y=");
      Serial.println(PONG_STATE.ball_y, DEC);
      close_response();
      break;

    case OPCODE_RESET:
      reset();
      Serial.println("message_type=reset_ack");
      Serial.print("id=");
      Serial.println(CURRENT_MESSAGE.id);
      close_response();
      break;

    case OPCODE_INIT_TIME:
      // cast CURRENT_MESSAGE to our time struct to conveniently fetch
      // out the current time
      WALL_TIME.raw = ((struct init_time*)(&CURRENT_MESSAGE))->cur_raw_time;
      WALL_TIME.initialized = 1;

      Serial.println("message_type=init_time_ack");
      Serial.print("id=");
      Serial.println(CURRENT_MESSAGE.id);
      close_response();
      break;

    case OPCODE_CURRENT_TIME:
      Serial.println("message_type=current_time_ack");
      Serial.print("id=");
      Serial.println(CURRENT_MESSAGE.id);
      close_response();
      break;

    default:
      Serial.println("message_type=unknown_command_ack");
      Serial.print("id=");
      Serial.println(CURRENT_MESSAGE.id);
      close_response();
      break;
  }
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
  static uint8_t cur_byte;
  static uint16_t* cur_word;
  static int8_t delimiter_index;
  static char buf[6];
  while (Serial.available() > 0) { // keep going as long as we might have messages
    cur_byte = (uint8_t)(Serial.read() & 0x000000ff);
    MESSAGE_BUFFER.buffer[(MESSAGE_BUFFER.count)++] = cur_byte;
    if (MESSAGE_BUFFER.count >= MESSAGE_SIZE) {
      if ((*(uint16_t*)(MESSAGE_BUFFER.buffer + MESSAGE_SIZE - 2)) != MESSAGE_DELIMITER) {
        // whoops, we got out of sync with the transmitter. Scan current
        // buffer for the delimiter, discard previous message, and shift
        // partial next message to front of buffer. This loses a message but
        // gets us back in sync
        delimiter_index = -2;
        for (int i = MESSAGE_SIZE - 2; i >= 0; --i) {
          if (*((uint16_t*)(MESSAGE_BUFFER.buffer + i)) == MESSAGE_DELIMITER) {
            if (((i - 1) >= 0) && (MESSAGE_BUFFER.buffer[i - 1] != MESSAGE_ESCAPE)) {
              delimiter_index = i;
              break;
            }
          }
        }
        MESSAGE_BUFFER.count = 0;
        if (delimiter_index >= 0) {
          for (int i = delimiter_index + 2; i < MESSAGE_SIZE; ++i, ++(MESSAGE_BUFFER.count)) {
            MESSAGE_BUFFER.buffer[MESSAGE_BUFFER.count] = MESSAGE_BUFFER.buffer[i];
          }
        }
        memset(MESSAGE_BUFFER.buffer + MESSAGE_BUFFER.count, 0, MESSAGE_SIZE - MESSAGE_BUFFER.count);
        close_response();
      } else {
        memcpy(&CURRENT_MESSAGE, MESSAGE_BUFFER.buffer, MESSAGE_SIZE);
        memset(&MESSAGE_BUFFER, 0, sizeof(MESSAGE_BUFFER));
      }
    }
  }
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
  static uint32_t tmp_prev_millis = 0;
  uint32_t tmp = 0;

  if (millis() / 1000 != tmp_prev_millis) {
    tmp_prev_millis = millis() / 1000;
  }

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
  pump_clock();
  pump_message_processor();
  handle_current_message();
}
