import java.util.Stack;

//**************** Binary Search Tree (integers) *****************

// Fill in the missing details in class Iter and TreeDriver.  

class Tree {
 public Tree(int n) { value = n; left = null; right = null;} 

 public void insert(int n) {
      if (value == n) return;
      if (value < n) 
	 if (right == null) right = new Tree(n);
         else right.insert(n);
      else if (left == null) left = new Tree(n);
           else left.insert(n);
 } 
	
 protected int value;
 protected Tree left;
 protected Tree right;
}

//http://manbroski.blogspot.com/2011/11/iterator-for-binary-search-tree.html
class Iter {
	public Iter(Tree root) {
		stack = new Stack<Tree>();
		stack.push(root);
		}
	public boolean done() {
		return stack.isEmpty();
	}
	public int next() { 
		//Deepth First Search
		currentNode = stack.pop();
		
		if(currentNode.right != null)
			stack.push(currentNode.right);
		
		if(currentNode.left != null)
			stack.push(currentNode.left);
		
		return currentNode.value;
	}
	
	protected Stack<Tree> stack;
	protected Tree currentNode;

}

class TreeDriver {
    static boolean lazyequal(Tree tree1, Tree tree2) {
    	Iter iter1 = new Iter(tree1);
    	Iter iter2 = new Iter(tree2);
    	
    	while((!iter1.done())&& (!iter2.done())){
    		if(iter1.next() != iter2.next()) {
    			return false;
    		}  			
    	}
    		   	
    	return (iter1.done() && iter2.done()) ;
    }

    public static void main (String args[]) throws java.io.IOException {
		Tree tree1 = new Tree(50);
		tree1.insert(10);
		tree1.insert(500);
		tree1.insert(100);
		tree1.insert(90);
		tree1.insert(200);
		tree1.insert(300);
		
		Tree tree2 = new Tree(50);
		tree2.insert(10);
		tree2.insert(100);
		tree2.insert(90);
		tree2.insert(200);
		tree2.insert(500);
		tree2.insert(300);

		boolean b = lazyequal(tree1,tree2);
	
		if(false == b) {
			System.out.println("tree1 tree 2 not equal");
		}
		else  {
			System.out.println("trer1 tree2 equal");
		}		
	   }
}