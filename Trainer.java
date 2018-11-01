import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Trainer {
	private static int ROUNDS = 30;
	private static int SPIN_STEP_DELAY = 2500;
	
	public static void main(String[] args) throws IOException {
		PlayerSkeleton p1 = new PlayerSkeleton();
		p1.learns = true;
		PlayerSkeleton p2 = new PlayerSkeleton();
		State s1, s2;
		TFrame frame1 = null;
		TFrame frame2 = null;
		double[] weights = new double[BasisFunction.FEATURE_COUNT];
		BasisFunction bf1 = p1.getBasisFunctions();
		double[] defWeights  = bf1.weight;
		double[] initialWeights = new double[BasisFunction.FEATURE_COUNT];
		System.arraycopy(defWeights, 0,weights, 0, BasisFunction.FEATURE_COUNT);
		String score = "";
		double bestAvg = 0;

		// keep on training!
		while(true) {
			int prevLength = 0;
			
			System.arraycopy(weights, 0,defWeights, 0, BasisFunction.FEATURE_COUNT);
			System.arraycopy(initialWeights, 0, defWeights, 0, BasisFunction.FEATURE_COUNT);
			System.out.println("Training for " + ROUNDS + " rounds...");
			double totalTrainingScore = 0;
			double totalTSSquared = 0;
			for(int i=0;i<ROUNDS;i++){

				s1 = new State();
				s2 = new State();
				s1.doublePlayer = true;
				s2.doublePlayer = true;
				if(frame1==null) frame1 = new TFrame(s1);
				else frame1.bindState(s1);
				if(frame2==null) frame2 = new TFrame(s2);
				else frame2.bindState(s2);
				playGame(s1,p1,s2,p2,score,i);
				double sent = ((double)s1.getTotalLinesSent()/s1.getTurnNumber());
				
				totalTrainingScore = totalTrainingScore + sent;
				totalTSSquared = totalTSSquared + (double)Math.pow(sent, 2);
				
				score = Double.toString(sent);
				System.out.print("\r  ");
				System.out.print(score);
				for(int j=0;j<=prevLength-score.length();j++) System.out.print(' ');
				prevLength = score.length();

			}
			
			double avg = (totalTrainingScore/(double)ROUNDS);
			double sd = (double)Math.sqrt((totalTSSquared - totalTrainingScore*avg)/(ROUNDS-1));
			System.out.print("\rAverage training score: ");
			System.out.print(avg);
			System.out.print(" s.d.: ");
			System.out.print(sd);
			
			
			System.out.println();
			
			if(avg>bestAvg) bestAvg = avg;
			
			bf1.computeWeights();
			for(int i=0;i<weights.length;i++) {
				weights[i] = 0.1 * weights[i] + 0.9 * defWeights[i];
				weights[i] = 0.001 * (weights[i]*(0.5 - Math.random()));
			}
			//System.out.println("Weights:"+Arrays.toString(bf.weight));
			System.out.println("Testing for "+ROUNDS+" rounds...");
			double totalTestingScore = 0;
			double totalTSquared = 0;
			prevLength = 0;
			for(int i=0;i<ROUNDS;i++){
				s1 = new State();
				s2 = new State();
				s1.doublePlayer = true;
				s2.doublePlayer = true;
				frame1.bindState(s1);
				frame2.bindState(s2);
				playGame(s1,p1,s2,p2,score,i);
				double sent = ((double)s1.getTotalLinesSent()/s1.getTurnNumber());
				totalTestingScore += sent;
				totalTSquared += Math.pow(sent,2);
				score = Double.toString(sent);
				System.out.print("\r  ");
				System.out.print(score);
				for(int j=0;j<=prevLength-score.length();j++) System.out.print(' ');
				prevLength = score.length();
			}
			double testAvg = ((double)totalTestingScore/ROUNDS);
			double testSd = (double)Math.sqrt((totalTSquared - totalTestingScore*avg)/(ROUNDS-1));
			System.out.print("\rAverage testing score: ");
			System.out.print(testAvg);
			System.out.print(" s.d.: ");
			System.out.print(testSd);
			System.out.println();
			
			if(testAvg > bestAvg) {
				bestAvg = testAvg;
				System.out.println("Test average better than best ever training average, swapping weights...");
			
				System.arraycopy(defWeights, 0, weights, 0, BasisFunction.FEATURE_COUNT);
				doNewWeightActions(weights);
			} else {
				System.arraycopy(defWeights, 0, initialWeights, 0, BasisFunction.FEATURE_COUNT);
			}
			
		}
	}
	
	private static void doNewWeightActions(double[] weights) {
		for(int i=0;i<weights.length;i++) {
			if(i != 0) System.out.println(',');
			System.out.print('\t');
			System.out.print(weights[i]);
		}
		System.out.println();

	}
	
	private static char[] rotating = new char[] {'-','\\','|','/'};
	private static Writer out = null;
	private static void playGame(State s1, PlayerSkeleton p1, State s2, PlayerSkeleton p2, String prevScore, int round) {
		int i = 0;
		int spin = 0;
		int[] bag = {0, 1, 2, 3, 4, 5, 6};
		int bag_index = 7;
		while(!s1.hasLost()&&!s2.hasLost()){
			if (bag_index < 0 || bag_index > 6) {
				Random rnd = ThreadLocalRandom.current();
				for (int k = bag.length - 1; k > 0; k--) {
					int index = rnd.nextInt(k + 1);
					// Simple swap
					int tmp = bag[index];
					bag[index] = bag[k];
					bag[k] = tmp;
				}
				bag_index = 0;
			}
			int nextPiece = bag[bag_index++];
			s1.setNextPiece(nextPiece);
			s2.setNextPiece(nextPiece);

			s1.makeMove(p1.pickMove(s1, s1.legalMoves()));
			s2.addlinesStack(s1.getLinesSent());
			s1.draw();
			s1.drawNext(0,0);
			s2.draw();
			s2.drawNext(0,0);
			String input1 = System.console().readLine();
			if (!s2.hasLost()) {
				s2.makeMove(p2.pickMove(s2, s2.legalMoves()));
				s1.addlinesStack(s2.getLinesSent());
				s1.draw();
				s1.drawNext(0,0);
				s2.draw();
				s2.drawNext(0,0);
				String input2 = System.console().readLine();
			}
			if(i == SPIN_STEP_DELAY) {
				System.out.print("\r");
				System.out.print(rotating[spin]);
				System.out.print(" Round ");
				System.out.print(round);
				System.out.print(": ");
				System.out.print(prevScore);
				spin = (spin+1)%rotating.length;
				i=0;
			}
			i++;
		}
		s1.draw();
		s1.drawNext(0,0);
		s2.draw();
		s2.drawNext(0,0);
	}

	private static void drawBoard(State s, TFrame t) {
		t.bindState(s);
		s.draw();
		s.drawNext(0,0);
	}
}
