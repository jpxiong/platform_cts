# Copyright 2010 The Android Open-Source Project
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License

# NOTE: this Makefile is derived from an example included in Arduino 0017.
# That Makefile bore no license, but the rest of the Arduino is generally LGPL
# v2 with upgrade option. Since it was unclear what the license for the
# Makefile itself was intended to be, this version uses ASL2.0, as LGPL is
# an unusual license choice for a Makefile. ASL2.0 is compatible with LGPL v3
# so there should be no conflict (due to the upgrade option.)

# Theory of Operation
# 'Arduino' refers to a hardware platform, and a software IDE. The IDE is
# pretty simple, and boils down to a crude text editor and some build
# plumbing. An Arduino 'sketch' is the core of a C (or C++) program; the build
# plumbing wraps a bunch of boilerplate (like #includes and entry point stubs)
# around the user's core code, and handles running the gcc cross-compiler and
# linking in the appropriate goodies to target AVR. It also handles
# programming the Atmel part on the board, via USB.
# 
# The function of this Makefile is simply to replicate all the boilerplate
# management. An Arduino 'sketch' is stored as a .pde file, but is essentially
# C code; so this Makefile cats together a full C/C++ file, compiles the
# standard Arduino libraries, and links them all together, using the AVR
# cross-compiler toolchain for gcc. (Ubuntu provides a pre-built avr-gcc
# package.)

# Path settings
TARGET = $(notdir $(CURDIR))
ARDUINO_PATH=/usr/share/arduino
ARDUINO_VERSION=18
ARDUINO_CORES_PATH = $(ARDUINO_PATH)/hardware/arduino/cores/arduino
AVR_TOOLS_PATH = /usr/bin
AVRDUDE_PATH = /usr/bin

# Programming port information
PORT = /dev/ttyUSB0
UPLOAD_RATE = 57600

# Target information (settings below work for Duemilanove)
AVRDUDE_PROGRAMMER = stk500v1
MCU = atmega328p
F_CPU = 16000000
OUT_DIR = out

# C-language libraries to compile and link with all programs
ARDUINO_C_MODULES_SRC =  \
$(ARDUINO_CORES_PATH)/wiring_pulse.c \
$(ARDUINO_CORES_PATH)/wiring_analog.c \
$(ARDUINO_CORES_PATH)/pins_arduino.c \
$(ARDUINO_CORES_PATH)/wiring.c \
$(ARDUINO_CORES_PATH)/wiring_digital.c \
$(ARDUINO_CORES_PATH)/WInterrupts.c \
$(ARDUINO_CORES_PATH)/wiring_shift.c
ARDUINO_C_MODULES_OBJ = \
$(OUT_DIR)/wiring_pulse.o \
$(OUT_DIR)/wiring_analog.o \
$(OUT_DIR)/pins_arduino.o \
$(OUT_DIR)/wiring.o \
$(OUT_DIR)/wiring_digital.o \
$(OUT_DIR)/WInterrupts.o \
$(OUT_DIR)/wiring_shift.o

# C++-language libraries to compile and link with all programs
ARDUINO_CPP_MODULES_SRC = \
$(ARDUINO_CORES_PATH)/Tone.cpp \
$(ARDUINO_CORES_PATH)/main.cpp \
$(ARDUINO_CORES_PATH)/WMath.cpp \
$(ARDUINO_CORES_PATH)/Print.cpp \
$(ARDUINO_CORES_PATH)/HardwareSerial.cpp
ARDUINO_CPP_MODULES_OBJ = \
$(OUT_DIR)/Tone.o \
$(OUT_DIR)/main.o \
$(OUT_DIR)/WMath.o \
$(OUT_DIR)/Print.o \
$(OUT_DIR)/HardwareSerial.o

TARGET_CPP_SRC = $(OUT_DIR)/$(TARGET).cpp
FORMAT = ihex

# Name of this Makefile (used for "make depend").
MAKEFILE = Makefile

# Debugging format.
# Native formats for AVR-GCC's -g are stabs [default], or dwarf-2.
# AVR (extended) COFF requires stabs, plus an avr-objcopy run.
DEBUG =
OPT = s

# Place -D or -U options here
CDEFS = -DF_CPU=$(F_CPU)L -DARDUINO=$(ARDUINO_VERSION)
CXXDEFS = -DF_CPU=$(F_CPU)L -DARDUINO=$(ARDUINO_VERSION)

# Place -I options here
CINCS = -I$(ARDUINO_CORES_PATH)
CXXINCS = -I$(ARDUINO_CORES_PATH)

# toolchain flags
CDEBUG = -g$(DEBUG)
CWARN = -w      # suppress all warnings
CTUNING = -ffunction-sections -fdata-sections
CXXTUNING = -fno-exceptions -ffunction-sections -fdata-sections
CFLAGS = $(CDEBUG) -O$(OPT) $(CWARN) $(CTUNING) $(CDEFS) $(CINCS) $(CSTANDARD)
CXXFLAGS = $(CDEBUG) -O$(OPT) $(CWARN) $(CXXTUNING) $(CDEFS) $(CINCS)
#ASFLAGS = -Wa,-adhlns=$(<:.S=.lst),-gstabs
LDFLAGS = -O$(OPT) -lm -Wl,--gc-sections

# Programming support using avrdude. Settings and variables.
AVRDUDE_WRITE_FLASH = -U flash:w:$(OUT_DIR)/$(TARGET).hex
#AVRDUDE_FLAGS = -V -F -C $(ARDUINO_PATH)/hardware/tools/avrdude.conf \

AVRDUDE_FLAGS = -V -F -p $(MCU) -P $(PORT) -c $(AVRDUDE_PROGRAMMER) -b $(UPLOAD_RATE)

# AVR cross-compiler toolchain binaries
CC = $(AVR_TOOLS_PATH)/avr-gcc
CXX = $(AVR_TOOLS_PATH)/avr-g++
LD = $(AVR_TOOLS_PATH)/avr-gcc
OBJCOPY = $(AVR_TOOLS_PATH)/avr-objcopy
OBJDUMP = $(AVR_TOOLS_PATH)/avr-objdump
AR  = $(AVR_TOOLS_PATH)/avr-ar
SIZE = $(AVR_TOOLS_PATH)/avr-size
NM = $(AVR_TOOLS_PATH)/avr-nm
AVRDUDE = $(AVRDUDE_PATH)/avrdude
 
# General programs
REMOVE = rm -f
RMDIR = rmdir
MV = mv -f

# Combine all necessary flags and optional flags.
# Add target processor to flags.
ALL_CFLAGS = $(CFLAGS) -mmcu=$(MCU)
ALL_CXXFLAGS = $(CXXFLAGS) -mmcu=$(MCU)
ALL_ASFLAGS = -x assembler-with-cpp $(ASFLAGS) -mmcu=$(MCU)
ALL_LDFLAGS = $(LDFLAGS) -mmcu=$(MCU)

# Default target.
all: $(OUT_DIR)_files build sizeafter
build: elf hex

# Concatenates the 'sketch' .pde file with the usual Arduino boilerplate
$(OUT_DIR)/$(TARGET).cpp: $(TARGET).pde
	@echo "Generate $(TARGET).cpp from $(TARGET).pde"
	@test -d $(OUT_DIR) || mkdir $(OUT_DIR)
	@echo '#include "WProgram.h"' > $(OUT_DIR)/$(TARGET).cpp
	@echo 'void setup();' >> $(OUT_DIR)/$(TARGET).cpp
	@echo 'void loop();' >> $(OUT_DIR)/$(TARGET).cpp
	@cat $(TARGET).pde >> $(OUT_DIR)/$(TARGET).cpp

elf: $(OUT_DIR)/$(TARGET).elf
hex: $(OUT_DIR)/$(TARGET).hex
eep: $(OUT_DIR)/$(TARGET).eep
lss: $(OUT_DIR)/$(TARGET).lss
sym: $(OUT_DIR)/$(TARGET).sym

# Program the device.  
upload: $(OUT_DIR)/$(TARGET).hex
	# pulsedtr strobes the DTR line to tell arduino to enter pgming mode
	python ./pulsedtr.py $(PORT)
	$(AVRDUDE) $(AVRDUDE_FLAGS) $(AVRDUDE_WRITE_FLASH)


# Display size of file.
HEXSIZE = $(SIZE) --target=$(FORMAT) $(OUT_DIR)/$(TARGET).hex
ELFSIZE = $(SIZE)  $(OUT_DIR)/$(TARGET).elf
sizebefore:
	@if [ -f $(OUT_DIR)/$(TARGET).elf ]; then echo $(MSG_SIZE_BEFORE); $(HEXSIZE); fi

sizeafter:
	@if [ -f $(OUT_DIR)/$(TARGET).elf ]; then echo $(MSG_SIZE_AFTER); $(HEXSIZE); fi


# Convert ELF to COFF for use in debugging / simulating in AVR Studio or VMLAB.
COFFCONVERT=$(OBJCOPY) --debugging \
--change-section-address .data-0x800000 \
--change-section-address .bss-0x800000 \
--change-section-address .noinit-0x800000 \
--change-section-address .eeprom-0x810000


coff: $(OUT_DIR)/$(TARGET).elf
	$(COFFCONVERT) -O coff-avr $(OUT_DIR)/$(TARGET).elf $(TARGET).cof


extcoff: $(TARGET).elf
	$(COFFCONVERT) -O coff-ext-avr $(OUT_DIR)/$(TARGET).elf $(TARGET).cof


.SUFFIXES: .elf .hex .eep .lss .sym

.elf.hex:
	$(OBJCOPY) -O $(FORMAT) -R .eeprom $< $@

.elf.eep:
	$(OBJCOPY) -O $(FORMAT) -j .eeprom --set-section-flags=.eeprom="alloc,load" \
	--no-change-warnings \
	--change-section-lma .eeprom=0 $< $@

# Create extended listing file from ELF output file.
.elf.lss:
	$(OBJDUMP) -h -S $< > $@

# Create a symbol table from ELF output file.
.elf.sym:
	$(NM) -n $< > $@

	# Link: create ELF output file from library.
$(OUT_DIR)/$(TARGET).elf: $(OUT_DIR)/$(TARGET).o $(OUT_DIR)/core.a
	$(LD) $(ALL_LDFLAGS) -o $@ $(OUT_DIR)/$(TARGET).o $(OUT_DIR)/core.a

$(OUT_DIR)/core.a: $(ARDUINO_C_MODULES_OBJ) $(ARDUINO_CPP_MODULES_OBJ)
	@for i in $(ARDUINO_C_MODULES_OBJ) $(ARDUINO_CPP_MODULES_OBJ); do echo $(AR) rcs $(OUT_DIR)/core.a $$i; $(AR) rcs $(OUT_DIR)/core.a $$i; done

$(ARDUINO_C_MODULES_OBJ):
	$(CC) -c $(ALL_CFLAGS) $(ARDUINO_CORES_PATH)/$(patsubst %.o,%.c,$(patsubst $(OUT_DIR)/%,%,$@)) -o $@

$(ARDUINO_CPP_MODULES_OBJ):
	$(CXX) -c $(ALL_CXXFLAGS) $(ARDUINO_CORES_PATH)/$(patsubst %.o,%.cpp,$(patsubst $(OUT_DIR)/%,%,$@)) -o $@

# Compile: create object files from C++ source files.
.cpp.o:
	$(CXX) -c $(ALL_CXXFLAGS) $< -o $@

# Compile: create object files from C source files.
.c.o:
	$(CC) -c $(ALL_CFLAGS) $< -o $@


# Compile: create assembler files from C source files.
.c.s:
	$(CC) -S $(ALL_CFLAGS) $< -o $@


# Assemble: create object files from assembler source files.
.S.o:
	$(CC) -c $(ALL_ASFLAGS) $< -o $@


# Automatic dependencies
%.d: %.c
	$(CC) -M $(ALL_CFLAGS) $< | sed "s;$(notdir $*).o:;$*.o $*.d:;" > $@

%.d: %.cpp
	$(CXX) -M $(ALL_CXXFLAGS) $< | sed "s;$(notdir $*).o:;$*.o $*.d:;" > $@


# Target: clean project.
clean:
	$(REMOVE) $(OUT_DIR)/$(TARGET).hex $(OUT_DIR)/$(TARGET).eep \
        $(OUT_DIR)/$(TARGET).cof $(OUT_DIR)/$(TARGET).elf \
        $(OUT_DIR)/$(TARGET).map $(OUT_DIR)/$(TARGET).sym \
        $(OUT_DIR)/$(TARGET).lss $(OUT_DIR)/core.a \
	$(ARDUINO_C_MODULES_SRC:.c=.s) $(ARDUINO_C_MODULES_SRC:.c=.d) \
        $(ARDUINO_CPP_MODULES_SRC:.cpp=.s) $(ARDUINO_CPP_MODULES_SRC:.cpp=.d) \
        $(ARDUINO_C_MODULES_OBJ) $(ARDUINO_CPP_MODULES_OBJ) \
        $(OUT_DIR)/$(TARGET).cpp $(OUT_DIR)/$(TARGET).o
distclean: clean
	$(RMDIR) $(OUT_DIR)

.PHONY: all build elf hex eep lss sym program coff extcoff clean $(OUT_DIR)_files sizebefore sizeafter

#include $(ARDUINO_C_MODULES_SRC:.c=.d)
#include $(ARDUINO_CPP_MODULES_SRC:.cpp=.d) 
#include $(TARGET_CPP_SRC:.cpp=.d) 
