// Heat2D.java, edited to follow program 2 instructions.

import mpi.MPI;
import mpi.MPIException;
import java.util.Date;

public class Heat2D_mpi {
    private static double a = 1.0;  // heat speed
    private static double dt = 1.0; // time quantum
    private static double dd = 2.0; // change in system

    public static void main( String[] args ) {
        // Verify arguments.
        if ( args.length != 4 ) {
            System.out.
                println( "usage: " +
                         "java Heat2D_mpi size max_time heat_time interval" );
            System.exit( -1 );
        }

        int size = Integer.parseInt( args[0] );
        int max_time = Integer.parseInt( args[1] );
        int heat_time = Integer.parseInt( args[2] );
        int interval  = Integer.parseInt( args[3] );
        double r = a * dt / ( dd * dd );

        try {
            // Initialize MPI.
            MPI.Init(args);
            int mpi_size = MPI.COMM_WORLD.Size(); // Number of nodes.
            int mpi_rank = MPI.COMM_WORLD.Rank(); // ID of "this" node.

            // Each rank is only responsible for calculating it's stripe. 
            int stripeLeftBoundary =  (size/mpi_size) * mpi_rank;
            int stripeRightBoundary = (size/mpi_size) * (mpi_rank + 1) - 1;
            int stripeSize = stripeRightBoundary - stripeLeftBoundary + 1;

            // Create a space in 1D array form:
            double[] z_1D = new double[2 * size * size];
            for ( int p = 0; p < 2; p++ ) {
                for ( int x = 0; x < size; x++ ) {
                    // Two upper rows are identical 
                    z_1D[p*size * size+x * size+0] = z_1D[p*size * size+x * size+1];

                    // Two lower rows are identical 
                    z_1D[p*size * size+x * size+(size-1)] = z_1D[p*size * size+x * size+(size-2)];

                    for ( int y = 0; y < size; y++ ) {
                        // Two leftmost columns are identical 
                        z_1D[p*size * size+0 * size+y] = z_1D[p*size * size+1 * size+y];

                        // Two rightmost columns are identical
                        z_1D[p*size * size+(size -1) * size+y]  = z_1D[p*size * size+(size-2) * size+y];
                    }
                }
            }

            
            // Start a timer.
            Date startTime = new Date( );

            // Simulate heat diffusion:
            for (int t = 0; t < max_time; t++ ) {
                int p = t % 2; // p = 0 or 1: indicates the phase
                int p2 = (p + 1) % 2;

                // First 3 loops are to be done simultanously (in each time step):

                // Loop 1: Make two left-most and two right-most columns identical.
                for ( int y = 0; y < size; y++ ) {
                    z_1D[p*size * size+0 * size+y] = z_1D[p*size * size+1 * size+y];
                    z_1D[p*size * size+(size-1) * size+y] = z_1D[p*size * size+(size-2) * size+y];
                }

                // Loop 2: Make two upper and lower rows identical.
                for ( int x = 0; x < size; x++ ) {
                    z_1D[p*size * size+x * size+0] = z_1D[p*size * size+x * size+1];
                    z_1D[p*size * size+x * size+(size-1)] = z_1D[p*size * size+x * size+(size-2)];
                }

                // Loop 3: Keep heating the bottom until t < heat_time.
                if ( t < heat_time ) {
                    for ( int x = size /3; x < size / 3 * 2; x++ ) {
                        z_1D[p*size * size+x * size+0] = 19.0; // Heat.
                    }
                }

                // After 3 loops, exchange boundary data between neighboring ranks.
                // Send and recieve to/from neighbors, unless boundary:

                if (mpi_rank > 0) { // Handle Left neighbor.
                    // Send the first column of this rank's stripe to the left neighbor
                    int offset = p * size * size + (stripeLeftBoundary * size);
                    MPI.COMM_WORLD.Send(z_1D, offset, size, MPI.DOUBLE, mpi_rank - 1, 0);
                    
                    // Receive the last column from the left neighbor and place it before the start of this rank's stripe
                    offset = p * size * size + ((stripeLeftBoundary - 1) * size);
                    MPI.COMM_WORLD.Recv(z_1D, offset, size, MPI.DOUBLE, mpi_rank - 1, 0);
                }
                
                if (mpi_rank + 1 < mpi_size) { // Handle Right neighbor.
                    // Send the last column of this rank's stripe to the right neighbor
                    int offset = p * size * size + (stripeRightBoundary * size);
                    MPI.COMM_WORLD.Send(z_1D, offset, size, MPI.DOUBLE, mpi_rank + 1, 0);
                
                    // Receive the first column from the right neighbor and place it after this rank's stripe
                    offset = p * size * size + (stripeRightBoundary + 1) * size;
                    MPI.COMM_WORLD.Recv(z_1D, offset, size, MPI.DOUBLE, mpi_rank + 1, 0);
                }                
                

                // On 4th loop, rank 0 collects data and prints status.
                if (mpi_rank != 0) {  // Ranks 1+ need to send their data to 0. 
                    int offset = p * size * size + (stripeLeftBoundary * size);
                    MPI.COMM_WORLD.Send(z_1D, offset, stripeSize * size, MPI.DOUBLE, 0, 0);

                } else { // Rank 0:
                    // Collect data from other ranks.
                    for (int node = 1; node < mpi_size; node++) {
                        int nodeLeftBound = stripeSize * node;
                        int offset = p * size * size + (nodeLeftBound * size);

                        // Receive the stripe from the node directly into the appropriate section of z_1D
                        MPI.COMM_WORLD.Recv(z_1D, offset, stripeSize * size, MPI.DOUBLE, node, 0);
                    }

                    // Display intermediate results.
                    if (interval != 0 && (t % interval == 0 || t == max_time - 1)) {
                        System.out.println( "Time = " + t );
        
                        for ( int y = 0; y < size; y++ ) {
                            for ( int x = 0; x < size; x++ ) {
                                System.out.print((int)(Math.floor(z_1D[p*size * size+x * size+y]/2) ) + " " );
                            }
        
                            System.out.println( );
                        }
                        System.out.println( );
                    }
                }

                // On 5th loop, each rank computes stripe using forward Euler
                for (int x = stripeLeftBoundary; x <= stripeRightBoundary; x++ ) {
                    if ((x == 0) || x == (size -1)) {
                        continue;
                    }
                    for (int y = 1; y < size - 1; y++) {
                            z_1D[p2*size * size+x * size+y] = z_1D[p*size * size+x * size+y] +
                                r * (z_1D[p*size * size+(x+1) * size+y] - 2 * z_1D[p*size * size+x * size+y] + z_1D[p*size * size+(x-1) * size+y]) +
                                r * (z_1D[p*size * size+x * size+(y+1)] - 2 * z_1D[p*size * size+x * size+y] + z_1D[p*size * size+x * size+(y-1)]);
                    }
                }

            } // End of simulation

            // Finalize MPI
            MPI.Finalize();

            // Finish the timer
            Date endTime = new Date( );
            System.out.println( "Elapsed time = " + (endTime.getTime( ) - startTime.getTime( )));

        } catch (MPIException mpiExc) {
            mpiExc.printStackTrace();
            
        }
    
    }
}