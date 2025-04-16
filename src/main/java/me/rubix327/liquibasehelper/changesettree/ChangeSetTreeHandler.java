package me.rubix327.liquibasehelper.changesettree;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;

import javax.swing.*;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;

public class ChangeSetTreeHandler extends JFrame {

    String     root  = "Корневая запись";
    String[]   nodes = new String[]  {"Напитки", "Сладости"};
    String[][] leafs = new String[][]{{"Чай", "Кофе", "Пиво", "Минералка"},
            {"Пирожное", "Мороженое", "Зефир", "Халва"}};

    public ChangeSetTreeHandler() {
        super("Пример использования JTree");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTree tree4 = new Tree(new SimpleModel());
        JPanel contents = new JPanel();
        JScrollPane scrollPane = new JBScrollPane(tree4,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        contents.add(scrollPane);
        setContentPane(contents);
        // Вывод окна на экран
        setSize(400, 300);
        setVisible(true);
    }

    // Модель данных дерева
    class SimpleModel implements TreeModel
    {
        // Список потомков корневой записи
        private ArrayList<String> rootList = new ArrayList<String>();
        // Дочерние узлы первого уровня
        private ArrayList<String>[] tnodes;

        public SimpleModel()
        {
            // Заполнение списков данными
            tnodes = (ArrayList<String>[]) new ArrayList<?>[nodes.length];
            for (int i = 0; i < nodes.length; i++) {
                rootList.add(nodes[i]);
                tnodes[i] = new ArrayList<>();
                for (int j = 0; j < leafs[i].length; j++) {
                    tnodes[i].add(leafs[i][j]);
                }
            }
        }
        // Функция получения корневого узла дерева
        @Override
        public Object getRoot() {
            return root;
        }
        // Функция получения потомка корневого узла
        private final int getRootChild(Object node)
        {
            int idx = -1;
            for (int i = 0; i < rootList.size(); i++){
                if (rootList.get(i) == node) {
                    idx = i;
                    break;
                }
            }
            return idx;
        }
        // Функция получения количество потомков узла
        @Override
        public int getChildCount(Object node)
        {
            int idx = getRootChild(node);
            if ( node == root )
                return rootList.size();
            else if ( node == rootList.get(idx))
                return tnodes[idx].size();
            return 0;
        }
        // Функция получения потомка узла по порядковому номеру
        @Override
        public Object getChild(Object node, int index)
        {
            int idx = getRootChild(node);
            if ( node == root )
                return rootList.get(index);
            else if ( node == rootList.get(idx))
                return tnodes[idx].get(index);
            return null;
        }
        // Функция получения порядкового номера потомка
        @Override
        public int getIndexOfChild(Object node, Object child)
        {
            int idx = getRootChild(node);
            if ( node == root )
                return rootList.indexOf(child);
            else if ( node == rootList.get(idx))
                return tnodes[idx].indexOf(child);
            return 0;
        }
        // Функция определения, является ли узел листом
        @Override
        public boolean isLeaf(Object node)
        {
            int idx = getRootChild(node);
            if ((idx >= 0) && tnodes[idx].contains(node))
                return true;
            else
                return false;
        }
        // Функция вызывается при изменении значения некоторого узла
        @Override
        public void valueForPathChanged(TreePath path, Object value) {}
        // Метод присоединения слушателя
        @Override
        public void addTreeModelListener(TreeModelListener tml) {}
        // Методы удаления слушателя
        @Override
        public void removeTreeModelListener(TreeModelListener tml) {}
    }

}
