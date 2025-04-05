**The full description is provided in the report.**

The program’s objective is to simulate heat diffusion over a 2D size*size square grid, where size is the number of points per edge. 
Each point in the square is represented by an integer indicating the unit’s temperature.
![image](https://github.com/user-attachments/assets/63797193-65e5-4046-9401-ed5922526859)

The program uses the Forward Euler method to simulate the heat diffusion using the finite difference method, where:
- (x,y)_t : The temperature of a point at time t.
  Depends on its own temperature, heat outflow, and heat inflow from its neighbors.
- r           : Coefficient based on the diffusion rate, where r=heat speed*(time quantum)/(change in system)^2 .


![image](https://github.com/user-attachments/assets/f303c24a-1e62-40e5-bba6-030262a0996a)

# Non-Parallelized Version (Provided)
![non_parallelized_image](https://github.com/user-attachments/assets/f3728e2b-08bd-4612-a5e1-bf5e43b5bd1a)

# Parallelized Version (Implemented)
The parallelized version follows the same logic but splits up the grid into columns for each node. 
![parallelized_image](https://github.com/user-attachments/assets/b95a2f29-7db5-4abd-955a-ef1d3f6a64f3)

Before computing the stripes values, each rank receives its neighbor’s boundary so it can calculate its own.

## Performance Differences for Different Node Counts
**Note:** We could decide to have only rank 0 print it’s execution time, but for curiosity’s sake we’re keeping all the prints. 
Additionally, have the number of prints reflect on the number of nodes helps provide execution proofs.

![node_perfromances](https://github.com/user-attachments/assets/5139a30b-61c0-4276-a537-6699ab16a124)

## Parallelization Discusssion
The program parallelizes the 2D heat diffusion simulation by dividing the computational domain into vertical stripes, 
each assigned to a different MPI process. Each process is responsible for updating heat values within its stripe 
using the Forward Euler method, which relies on temperature values from adjacent cells. 

Note: To handle boundaries between stripes, each process exchanges boundary data with its neighbors at each time step, 
since each point relies on its neighbor’s temperature to calculate its own.

This allows each process to simultaneously  work on a smaller portion of the array, which should reduce the computation time.

## Limitations
At first, there were a few unexpected results where less nodes meant less elapsed time.
With more tests, it became evident that the issue became worse the more timestamps there were. 
This possibly meant that the communication time for the lab nodes is expensive.
![node_counts_times](https://github.com/user-attachments/assets/8c986682-fcac-4a67-a241-d2d4366b8716)

**Possible Improvements**
Generally, when the data set is smaller, using too many nodes is counter-productive because the 
communication costs outweigh the gains in computation costs.

On lowering the communication amount (smaller time variables), and larger windows 
(meaning each node does more work), results looks more as expected.

However the elapsed time for the same exact command could be hundreds off between tests, 
so using a more reliable system would improve test times. 

