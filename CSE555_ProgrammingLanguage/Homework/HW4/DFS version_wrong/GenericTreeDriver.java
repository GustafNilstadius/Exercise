import java.util.Stack;

//*********** Generic Binary Search Tree ****************

// Fill in the details for IntElem, Iter and GenericTreeDriver

class gTree<T extends Comparable<T>> {
	public gTree(T v) {
		value = v;
		left = null;
		right = null;
	}

	public void insert(T v) {
		if (value.compareTo(v) == 0)
			return;
		if (value.compareTo(v) > 0)
			if (left == null)
				left = new gTree<T>(v);
			else
				left.insert(v);
		else if (value.compareTo(v) < 0)
			if (right == null)
				right = new gTree<T>(v);
			else
				right.insert(v);
	}

	protected T value;
	protected gTree<T> left;
	protected gTree<T> right;
}

class IntElem implements Comparable<IntElem> {
    public IntElem(int v) { 
		val = v; 
    }
    public int value() { return val; }
    public int compareTo(IntElem i) { 
		return val - i.value(); 
     }
     private int val;
}

class gIter<T extends Comparable<T>> {
	protected Stack<gTree<T>> stack;
	protected gTree<T> currentNode;
	
	public gIter(gTree<T> root) {
		stack = new Stack<gTree<T>>();
		stack.push(root);
		}
	public boolean done() {
		return stack.isEmpty();
	}
	public T next() { 
		//Deepth First Search
		currentNode = stack.pop();
		
		if(currentNode.right != null)
			stack.push(currentNode.right);
		
		if(currentNode.left != null)
			stack.push(currentNode.left);
		
		return currentNode.value;
	}

}

class GenericTreeDriver {

	static <T extends Comparable<T>> boolean lazyequal(gTree<T> tree1, gTree<T> tree2) {
    	gIter<T> iter1 = new gIter<T>(tree1);
    	gIter<T> iter2 = new gIter<T>(tree2);
    	
    	while((!iter1.done())&& (!iter2.done())){
    		if(iter1.next() != iter2.next()) {
    			return false;
    		}  			
    	}
    		   	
    	return (iter1.done() && iter2.done()) ;
    }
	
    public static void main(String[] args) {
		gTree<IntElem> tree1 = new gTree<IntElem>(new IntElem(50));
		tree1.insert(new IntElem(10));
		tree1.insert(new IntElem(500));
		tree1.insert(new IntElem(100));
		tree1.insert(new IntElem(90));
		tree1.insert(new IntElem(200));
		tree1.insert(new IntElem(300));
		
		gTree<IntElem> tree2 = new gTree<IntElem>(new IntElem(100));
		tree2.insert(new IntElem(90));
		tree2.insert(new IntElem(200));
		tree2.insert(new IntElem(50));
		tree2.insert(new IntElem(300));
		tree2.insert(new IntElem(500));
		tree2.insert(new IntElem(10));		
		boolean b = lazyequal(tree1,tree2);
		
		if(false == b) {
			System.out.println("tree1 tree 2 not equal");
		}
		else  {
			System.out.println("trer1 tree2 equal");
		}		
	}
}
    
 
