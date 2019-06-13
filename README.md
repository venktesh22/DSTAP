# DSTAP
Working code for decentralized static traffic assignment with and without subnetwork artificial links

# File structure
All input and output files are organized inside the Networks/ folder, which is the only folder where files will have to be manipulated.

Each network has its own subfolder inside the Networks/ folder. Inside each network folder, we have two folders, Inputs/ and Outputs/, which as the names suggest store the input and output files from the DSTAP run.

Inputs/ folder requires following files:
* regionalLinks.txt: stores the link information for links which are not completely contained in any subnetwork (in simplified Bar-Gera format storing only the BPR function information)
* subnetNames.txt: Names of each subnetwork. Can be indexed by number or any custom name. These names are used to access the files for each subnetwork (like if the names are A and B, then A_net.txt stores the network information for subnetwork A, and likewise.
* Parameters.txt: stores the global parameters for running DSTAP as heuristic
* A_net, AIn_trips, AOut_trips: these files store the network information for each subnetwork. \_net.txt stores link information for all links contained within subnetwork A. In_trips.txt stores trips from origin to destination where both origin and destination are part of subnetwork A. Out_trips.txt stores trip information for all trips where origin is within the subnetwork but destination is outside of the subnetwork.

Each run generates a unique folder inside the Outputs/ folder (folder name is based on the current system time at start of the run). The run copies the Inputs/Parameters.txt file into the folder. The other output files generate the variation of relative gap and excess costs for each network (master network, each subnetwork, and the full network), and the link flow and OD travel time for each link and OD pair in the full network.

# Running the code
If using an IDE, ensure that following arguments are provided at run time: "network folder name (string)" "demand factor (float) that scales the demand by this multiple" "whether or not subnetworks should be run in parallel (true or false)". For example, to run Grid_2 network with base demand level without running subnetworks in parallel, use following run time arguments: `Grid_2 1.0 false`

If using external commands, compile the code to generate .jar files. More instructions will be provided in the future.
