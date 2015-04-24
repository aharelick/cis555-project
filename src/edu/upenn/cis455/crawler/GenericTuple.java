package edu.upenn.cis455.crawler;

public class GenericTuple<X,Y> {
	public final X left;
	public final Y right;
	public GenericTuple(X x, Y y) {
		left = x;
		right = y;
	}

}
