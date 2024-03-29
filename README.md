This compresses a sequence xs by finding the least costly[1] program that has xs as a prefix of its output.  It does so by considering the empty program, then all programs of length 1 (said to be the "successors" of the empty program), then all programs of length 2, and so on until it has considered all possible programs (except for some provably stupid ones) shorter than xs.

Note that this is a finite process if such a program exists (if the program length were not bounded, this condition would not be necessary).  All programs (of all these lengths) are stepped through in some sense in parallel.  At each point, the program with the lowest cost gets to execute one instruction.

This is not necessarily a dumb idea[2], though it will utterly devour your memory.  Some heuristics are in place to increase the search depth that can be reached before the VM blows up:

* Programs that are about to give output are inifinitely prioritized, because output, if inconsistent with the target sequence xs, allows us to disregard the program and any of its successors.  The prioritization allows this to be detected earlier, avoiding the need to keep around a program and its state in the queue unnecessarily.
* Programs are not always immediately replaced by their successors after they finish; we only branch when there is room in the successor queue.
* Programs that take more than 2^n time where n is the size of the input xs, are thrown away.
* Programs get n bytes of memory.
* Some very naive pruning is done to limit the number of pointless duplicates; programs containing "<>", "^v", "xx", "[]" etc are not considered.

The programs are in a brainfuck-inspired language.  Here's the instruction set:

* <, > to move the memory pointer
* ^, v to increment/decrement the value in the current cell
* [, ] to loop like in brainfuck
* . to output the value in the current cell
* @ to set the memory pointer to the value in the current cell
* ! to set the instruction pointer to the value in the current cell
* +, -, *, /, |, &, x, the usual binary operators (x is bitwise xor) that combine the values in the current cell and the cell to the left, and store the result in the current cell.

The extensions are intended to make the language a bit more concise than brainfuck.  They do, but it's still far from concise.  Brainfuck instructions just don't convey much.  What's needed is a more expressive language, preferably one that allows you to prove a lot of useful things about its programs.

[1] The "cost" of a program is defined as the sum of its length and the binary logarithm of its execution time.  The length is measured in bits -- instructions are byte-sized but carry only four bits of information, so could easily be compressed further.

[2] Actually, it looks like this is almost identical to http://www.scholarpedia.org/article/Universal_search
