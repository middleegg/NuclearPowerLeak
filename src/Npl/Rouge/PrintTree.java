package Npl.Rouge;

import arc.util.Log;

public class PrintTree{
    public static void main(String[] args){
        RougeTree.generateTree(12345L, 5);
        RougeTree.Node root = RougeTree.getRoot();
        if(root == null){
            Log.info("Root is null!");
            return;
        }
        Log.info("=== 关卡编号列表 (seed=12345, maxDepth=5) ===\n");
        printNode(root, "", true);
    }

    static void printNode(RougeTree.Node node, String prefix, boolean isLast){
        if(node == null) return;

        String branchLabel = "";
        if(node.parent != null){
            if(node.parent.left == node) branchLabel = "[左]";
            else if(node.parent.middle == node) branchLabel = "[中]";
            else if(node.parent.right == node) branchLabel = "[右]";
        }else{
            branchLabel = "[根]";
        }

        Log.info(prefix + (isLast ? "└── " : "├── ") + node.id + " " + branchLabel + " " + node.mapName);

        String newPrefix = prefix + (isLast ? "    " : "│   ");

        int childCount = 0;
        if(node.left != null) childCount++;
        if(node.middle != null) childCount++;
        if(node.right != null) childCount++;

        int i = 0;
        if(node.left != null){ i++; printNode(node.left, newPrefix, i == childCount); }
        if(node.middle != null){ i++; printNode(node.middle, newPrefix, i == childCount); }
        if(node.right != null){ i++; printNode(node.right, newPrefix, i == childCount); }
    }
}
