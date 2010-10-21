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
#define MESSAGE_SIZE 128
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
  uint16_t paddle_width;
  uint16_t paddle_offset;
  uint16_t max_paddle_motion;
  uint8_t unused[MESSAGE_SIZE - 14];
};

#define OPCODE_PONG_BALL_STATE (OPCODE_RESET + 4)
struct pong_ball_state {
  uint16_t opcode;
  uint16_t id;
  uint16_t ball_x;
  uint16_t ball_y;
  uint8_t unused[MESSAGE_SIZE - 8];
};

struct wall_time_struct {
  uint32_t raw; // long == 4-byte on AVR
  uint8_t initialized;
  uint8_t hours;
  uint8_t minutes;
  uint8_t seconds;
};
struct wall_time_struct WALL_TIME;

struct pong_state_struct {
  uint16_t playfield_width;
  uint16_t playfield_height;
  uint16_t paddle_width;
  uint16_t paddle_offset;
  uint16_t max_paddle_motion;
  uint16_t paddle_x;
  uint16_t last_ball_x;
  uint16_t last_ball_y;
};
struct pong_state_struct PONG_STATE;


void print_current_time() {
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
}


void handle_current_message() {
  static uint16_t last_id;
  static struct setmode_pong* setmode_pong_msg;
  static struct pong_ball_state* pong_ball_state_msg;
  static uint16_t paddle_half_width;
  static uint16_t paddle_max;
  static uint16_t danger;
  static uint8_t invert;
  static uint16_t delta;

  if (CURRENT_MESSAGE.id == 0 || CURRENT_MESSAGE.id == last_id) {
    return;
  }
  last_id = CURRENT_MESSAGE.id;

  switch (CURRENT_MESSAGE.opcode) {

    case OPCODE_SETMODE_PONG:
      memset(&PONG_STATE, 0, sizeof(PONG_STATE));
      setmode_pong_msg = (struct setmode_pong*)(&CURRENT_MESSAGE);
      PONG_STATE.playfield_width = setmode_pong_msg->playfield_width;
      PONG_STATE.playfield_height = setmode_pong_msg->playfield_height;
      PONG_STATE.paddle_width = setmode_pong_msg->paddle_width;
      PONG_STATE.paddle_offset = setmode_pong_msg->paddle_offset;
      PONG_STATE.max_paddle_motion = setmode_pong_msg->max_paddle_motion;

      paddle_half_width = PONG_STATE.paddle_width / 2;
      paddle_max = PONG_STATE.playfield_width - paddle_half_width;

      Serial.println("message_type=setmode_pong_ack");
      Serial.print("id=");
      Serial.println(CURRENT_MESSAGE.id);
      print_current_time();
      Serial.println("");
      break;

    case OPCODE_PONG_BALL_STATE:
      pong_ball_state_msg = (struct pong_ball_state*)(&CURRENT_MESSAGE);
      danger = pong_ball_state_msg->ball_x - PONG_STATE.paddle_x;
      invert = (danger < 0);
      danger *= invert ? -1 : 1;
      if (danger < paddle_half_width) {
        delta = 0;
      } else if (danger < PONG_STATE.playfield_width / 3) {
        delta = PONG_STATE.max_paddle_motion / 3;
      } else if (danger < PONG_STATE.playfield_width * 2 / 3) {
        delta = PONG_STATE.max_paddle_motion * 2 / 3;
      } else {
        delta = PONG_STATE.max_paddle_motion;
      }
      delta *= invert ? 1 : -1;
      PONG_STATE.paddle_x += delta;
      if (PONG_STATE.paddle_x < paddle_half_width) {
        PONG_STATE.paddle_x = paddle_half_width;
      } else if (PONG_STATE.paddle_x > paddle_max) {
        PONG_STATE.paddle_x = paddle_max;
      }

      Serial.println("message_type=pong_paddle_state");
      Serial.print("id=");
      Serial.println(CURRENT_MESSAGE.id);
      print_current_time();
      Serial.print("paddle_x=");
      Serial.println(PONG_STATE.paddle_x);
      Serial.println("");
      break;

    default:
      break;
  }
}

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
  static char buf[4];
  while (Serial.available() > 0) { // keep going as long as we might have messages
    cur_byte = (uint8_t)(Serial.read() & 0x000000ff);
    MESSAGE_BUFFER.buffer[(MESSAGE_BUFFER.count)++] = cur_byte;
    Serial.print("booga ");
    Serial.print(itoa(MESSAGE_BUFFER.count, buf, 10));
    Serial.print(" ");
    Serial.print(itoa(Serial.available(), buf, 10));
    Serial.print(" ");
    Serial.println(itoa(cur_byte, buf, 10));
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
        Serial.print("klaxon ");
        Serial.println(itoa(delimiter_index, buf, 10));
        Serial.print("klaxon ");
        Serial.println(itoa(*((uint16_t*)(MESSAGE_BUFFER.buffer + MESSAGE_SIZE - 2)), buf, 10));
        MESSAGE_BUFFER.count = 0;
        if (delimiter_index >= 0) {
          for (int i = delimiter_index + 2; i < MESSAGE_SIZE; ++i, ++(MESSAGE_BUFFER.count)) {
            MESSAGE_BUFFER.buffer[MESSAGE_BUFFER.count] = MESSAGE_BUFFER.buffer[i];
          }
        }
        memset(MESSAGE_BUFFER.buffer + MESSAGE_BUFFER.count, 0, MESSAGE_SIZE - MESSAGE_BUFFER.count);
      } else {
        memcpy(&CURRENT_MESSAGE, MESSAGE_BUFFER.buffer, MESSAGE_SIZE);
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

            Serial.println("message_type=init_time_ack");
            Serial.print("id=");
            Serial.println(CURRENT_MESSAGE.id);
            print_current_time();
            Serial.println("");

            CURRENT_MESSAGE.id = 0;
            break;

          case OPCODE_CURRENT_TIME:
            Serial.println("message_type=current_time_ack");
            Serial.print("id=");
            Serial.println(CURRENT_MESSAGE.id);
            print_current_time();
            Serial.println("");

            CURRENT_MESSAGE.id = 0;

          default:
            // no-op -- actually means main loop will handle it
            break;
        }
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
    print_current_time();
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
