package org.gicentre.aidan.ramp;



import java.util.Comparator;

public class ValuePair<E1,E2>{

	public E1 v1;
	public E2 v2;
	
	public ValuePair(E1 value1, E2 value2){
		this.v1=value1;
		this.v2=value2;
	}

	static public Comparator<ValuePair> getValue1Comparator(){
		return new Value1Comparator();
	}

	static public Comparator<ValuePair> getValue2Comparator(){
		return new Value2Comparator();
	}

	static public Comparator<ValuePair> getValue1DescendingComparator(){
		return new Value1DescendingComparator();
	}

	static public Comparator<ValuePair> getValue2DescendingComparator(){
		return new Value2DescendingComparator();
	}

	
	static public class Value1Comparator<E1,E2> implements Comparator<ValuePair<E1,E2>>{

		public int compare(ValuePair<E1,E2> o1, ValuePair<E1,E2> o2) {
			int result = 0;
			if (o1.v1 instanceof Comparable && o2.v1 instanceof Comparable){
				result = (((Comparable)(o1.v1)).compareTo((Comparable)(o2.v1)));
				if (result==0){
					if (o1.v2 instanceof Comparable && o2.v2 instanceof Comparable){
						result = (((Comparable)(o1.v2)).compareTo((Comparable)(o2.v2)));						
					}
				}

			}
			return result;
		}
	}

	static class Value1DescendingComparator<E1,E2> implements Comparator<ValuePair<E1,E2>>{

		public int compare(ValuePair<E1,E2> o1, ValuePair<E1,E2> o2) {
			int result = 0;
			if (o1.v1 instanceof Comparable && o2.v1 instanceof Comparable){
				result = (((Comparable)(o1.v1)).compareTo((Comparable)(o2.v1)));
				if (result==0){
					if (o1.v2 instanceof Comparable && o2.v2 instanceof Comparable){
						result = (((Comparable)(o1.v2)).compareTo((Comparable)(o2.v2)));						
					}
				}

			}
			return -result;
		}

	}

	static class Value2Comparator<E1,E2> implements Comparator<ValuePair<E1,E2>>{

		public int compare(ValuePair<E1,E2> o1, ValuePair<E1,E2> o2) {
			int result = 0;
			if (o1.v1 instanceof Comparable && o2.v1 instanceof Comparable){
				result = (((Comparable)(o1.v2)).compareTo((Comparable)(o2.v2)));
				if (result==0){
					if (o1.v2 instanceof Comparable && o2.v2 instanceof Comparable){
						result = (((Comparable)(o1.v1)).compareTo((Comparable)(o2.v1)));						
					}
				}

			}
			return result;
		}
	}

	static class Value2DescendingComparator<E1,E2> implements Comparator<ValuePair<E1,E2>>{

		public int compare(ValuePair<E1,E2> o1, ValuePair<E1,E2> o2) {
			int result = 0;
			if (o1.v1 instanceof Comparable && o2.v1 instanceof Comparable){
				result = (((Comparable)(o1.v2)).compareTo((Comparable)(o2.v2)));
				if (result==0){
					if (o1.v2 instanceof Comparable && o2.v2 instanceof Comparable){
						result = (((Comparable)(o1.v1)).compareTo((Comparable)(o2.v1)));						
					}
				}

			}
			return -result;
		}
	}
}		



