Add Grid Topology into the code.

Current configuration: 
1 GridMaster

4 nodes with 2 dimensions

0 (row master) 1 (column master)

2 (column master) 3 (row master)

How to Run:
#Run gridmaster
./bidmach scripts/testAllReduceGridMaster.scala 

#Run nodes (run the command 4 times on separate consoles)
./bidmach scripts/testAllReduceNode_2.scala 

The output is fairly self-explanatory.

To do:
Test the code!


