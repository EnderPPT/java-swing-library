package edu.training.library.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

final class Ui {
    static final Color INK = new Color(31, 41, 55);
    static final Color MUTED = new Color(100, 116, 139);
    static final Color PRIMARY = new Color(18, 103, 106);
    static final Color SURFACE = new Color(247, 249, 250);
    private Ui() {}

    static JPanel panel(LayoutManager layout) { JPanel p=new JPanel(layout);p.setBorder(new EmptyBorder(18,20,18,20));p.setBackground(SURFACE);return p; }
    static JLabel title(String text) { JLabel l=new JLabel(text);l.setFont(l.getFont().deriveFont(Font.BOLD,22f));l.setForeground(INK);return l; }
    static JLabel muted(String text) { JLabel l=new JLabel(text);l.setForeground(MUTED);return l; }
    static JButton primary(String text) { JButton b=new JButton(text);b.setBackground(PRIMARY);b.setForeground(Color.WHITE);return b; }
    static DefaultTableModel model(String...columns){return new DefaultTableModel(columns,0){@Override public boolean isCellEditable(int r,int c){return false;}};}
    static JScrollPane table(DefaultTableModel model, JTable[] holder) { JTable t=new JTable(model);t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);t.setAutoCreateRowSorter(true);t.getTableHeader().setReorderingAllowed(false);holder[0]=t;return new JScrollPane(t); }
    static JPanel toolbar(Component...items){JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));p.setOpaque(false);for(Component i:items)p.add(i);return p;}
    static JPanel form(String[] labels,JComponent[] fields){JPanel p=new JPanel(new GridBagLayout());GridBagConstraints g=new GridBagConstraints();g.insets=new Insets(6,6,6,6);g.fill=GridBagConstraints.HORIZONTAL;for(int i=0;i<labels.length;i++){g.gridx=0;g.gridy=i;g.weightx=0;p.add(new JLabel(labels[i]),g);g.gridx=1;g.weightx=1;p.add(fields[i],g);}return p;}
    static int selected(JTable table){int row=table.getSelectedRow();return row<0?-1:table.convertRowIndexToModel(row);}
    static void info(Component parent,String message){JOptionPane.showMessageDialog(parent,message,"提示",JOptionPane.INFORMATION_MESSAGE);}
    static void error(Component parent,Exception e){String m=e.getMessage();if(m==null&&e.getCause()!=null)m=e.getCause().getMessage();JOptionPane.showMessageDialog(parent,m==null?"操作失败":m,"操作失败",JOptionPane.ERROR_MESSAGE);}
    static boolean confirm(Component parent,String message){return JOptionPane.showConfirmDialog(parent,message,"确认",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)==JOptionPane.YES_OPTION;}
    static JPanel stack(List<? extends Component> rows){JPanel p=new JPanel();p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));for(Component row:rows){row.setMaximumSize(new Dimension(Integer.MAX_VALUE,row.getPreferredSize().height));p.add(row);p.add(Box.createVerticalStrut(10));}return p;}
}
