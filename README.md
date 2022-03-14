# ISA.-Assembler-disassembler.

## Information
A translator program that can be used to convert machine code into assembly language program text.

ISA: RISC-V RV32I, RV32M, RVC.
The RVC can be found here: waterman-ms.pdf (berkeley.edu)
Encoding: little endian.
ELF file: 32 bits.
Only sections .text, .symtable are processed.
For each line of code, its address is indicated in hex format (16 CC).
The designation of the labels must be found in the Symbol Table (.symtable). If the label name is not found there, then the following notation is used: LOC_%05x.

## Input format
Arguments are passed to the program via the command line:
hw4.exe <input_elf_filename> <output_filename>

## Output Format
At the output, you get the image processed by the algorithm, and the measurement results are displayed on the console. 
  
Output format: "Time (%i thread(s)): %g ms\n"
