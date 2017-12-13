package self.ed;

import self.ed.exception.MultipleSolutionsException;
import self.ed.exception.NoSolutionException;
import self.ed.exception.TimeLimitException;

import java.util.*;

import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.rangeClosed;
import static self.ed.SudokuUtils.copy;

public class SudokuSolver {
    private Integer[][] solution;
    private List<Cell> pendingCells = new ArrayList<>();

    public SudokuSolver(Integer[][] initialValues) {
        int size = initialValues.length;
        int blockSize = (int) Math.sqrt(size);
        Set<Integer> values = rangeClosed(1, size).boxed().collect(toSet());

        solution = new Integer[size][size];
        Map<Cell, Integer> openCells = new HashMap<>();
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int block = blockSize * (row / blockSize) + col / blockSize;
                Integer value = initialValues[row][col];
                if (value != null) {
                    openCells.put(new Cell(row, col, block, emptySet()), value);
                } else {
                    pendingCells.add(new Cell(row, col, block, values));
                }
            }
        }

        openCells.forEach(this::open);
    }

    public Integer[][] solve() {
        if (Thread.interrupted()) {
            throw new TimeLimitException();
        }

        while (!pendingCells.isEmpty()) {
            Cell cell = pendingCells.get(0);
            if (cell.countCandidates() != 1) {
                pendingCells.sort(comparing(Cell::countCandidates));
                cell = pendingCells.get(0);
            }
            if (cell.countCandidates() == 1) {
                open(cell, cell.getCandidate());
            } else if (cell.countCandidates() > 1) {
                System.out.println("Guessing out of " + cell.countCandidates());
                List<Integer[][]> solutions = new ArrayList<>();
                for (Integer value : cell.getCandidates()) {
                    Integer[][] nextGuess = copy(solution);
                    nextGuess[cell.getRow()][cell.getCol()] = value;
                    try {
                        solutions.add(new SudokuSolver(nextGuess).solve());
                        if (solutions.size() > 1) {
                            throw new MultipleSolutionsException();
                        }
                    } catch (NoSolutionException e) {
                        // Our guess did not work, let's try another one
                    }
                }
                if (solutions.isEmpty()) {
                    throw new NoSolutionException();
                }
                return solutions.iterator().next();
            } else {
                throw new NoSolutionException();
            }
        }

        return solution;
    }

    private void open(Cell cell, Integer value) {
        solution[cell.getRow()][cell.getCol()] = value;
        pendingCells.remove(cell);
        pendingCells.stream().filter(cell::isRelated).forEach(pend -> pend.removeCandidate(value));
    }
}
