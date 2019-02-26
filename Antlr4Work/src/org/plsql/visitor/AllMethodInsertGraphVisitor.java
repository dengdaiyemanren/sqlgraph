package org.plsql.visitor;

import java.io.File;
import java.text.Format;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.generated.PlSqlParser;
import org.antlr.generated.PlSqlParser.Function_callContext;
import org.antlr.generated.PlSqlParser.Function_specContext;
import org.antlr.generated.PlSqlParser.Id_expressionContext;
import org.antlr.generated.PlSqlParser.Package_bodyContext;
import org.antlr.generated.PlSqlParser.Package_nameContext;
import org.antlr.generated.PlSqlParser.Package_specContext;
import org.antlr.generated.PlSqlParser.Procedure_specContext;
import org.antlr.generated.PlSqlParser.Routine_clauseContext;
import org.antlr.generated.PlSqlParser.StatementContext;
import org.antlr.generated.PlSqlParser.Unit_statementContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.graphviz.GraphViz;
import org.plsql.PlSqlParserTree;
import org.plsql.utils.PlSqlUtils;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubSelect;

public class AllMethodInsertGraphVisitor extends PlSqlRuleVisitor {
    public enum ExpressionType {
        PACKAGE, FUNCTION, PROCEDURE, VARIABLE, UNKNOWN
    }

    public enum AllMethodVistFlag {
        FIRST_PASS, SECOND_PASS
    }

    public AllMethodVistFlag visitMode = AllMethodVistFlag.FIRST_PASS;

    public static Map<String, ExpressionType> idMap = new HashMap<String, ExpressionType>();
    private String currentSchema = null;
    
    public String currentTable  = null;
    
    public HashMap<String,GraphViz> tableAndGraph = new HashMap<String,GraphViz>();
    

    public AllMethodInsertGraphVisitor(PlSqlParserTree tree) {
        super(tree);
    }

    private void logContext(ParserRuleContext ctx) {
        System.out.printf("%04d > %s\n",ctx.start.getLine(), PlSqlUtils.getOriginalText(input, ctx));
    }

    private String getFullProcName(ParserRuleContext ctx) {
        ParserRuleContext c = ctx;

        String fullName = "";

        //fullName = ctx.getText();

        while (c != null) {
            if (c instanceof Function_specContext) {
                Function_specContext fc = (Function_specContext) c;
                fullName = PlSqlUtils.getOriginalText(input, fc.function_name()) + "->Type=FUNCTION:" + fullName;
            } else if (c instanceof Procedure_specContext) {
                Procedure_specContext pc = (Procedure_specContext) c;
                fullName = PlSqlUtils.getOriginalText(input, pc.procedure_name()) + "->Type=PROCEDURE:" + fullName;
            } else if (c instanceof Package_bodyContext) {
                Package_bodyContext pbc = (Package_bodyContext) c;
                // TODO: здесь индекс у списка пакетов!
                fullName = PlSqlUtils.getOriginalText(input, pbc.package_name(0)) + "->TYPE=PACKAGE:" + fullName;
                break;
            }
            c = c.getParent();
        }

        return fullName;
    }

    @Override
    public Void visitPackage_name(Package_nameContext ctx) {
        if (visitMode == AllMethodVistFlag.FIRST_PASS) {
            String name = PlSqlUtils.getOriginalText(input, ctx);
            System.out.println(PlSqlUtils.getLine(ctx) + " PACKAGE " + name);

            currentSchema = PlSqlUtils.getOriginalText(input, ctx.schema_name());
            System.out.println("  SCHEMA: " + currentSchema);

            idMap.put(name, ExpressionType.PACKAGE);
            //        logContext(ctx);
        }

        return super.visitPackage_name(ctx);
    }

    @Override
    public Void visitFunction_spec(Function_specContext ctx) {
        if (visitMode == AllMethodVistFlag.FIRST_PASS) {
            String name = getFullProcName(ctx/*.function_name()*/);
            System.out.println(PlSqlUtils.getLine(ctx) + " FUNCTION " + name);

            idMap.put(name, ExpressionType.FUNCTION);
            //        logContext(ctx);
        }
        return super.visitFunction_spec(ctx);
    }

    @Override
    public Void visitProcedure_spec(Procedure_specContext ctx) {
        if (visitMode == AllMethodVistFlag.FIRST_PASS) {
            String name = getFullProcName(ctx/*.procedure_name()*/);
            System.out.println(PlSqlUtils.getLine(ctx) + " PROCEDURE " + name);

            idMap.put(name, ExpressionType.PROCEDURE);
            //        logContext(ctx);
        }
        return super.visitProcedure_spec(ctx);
    }

    @Override
    public Void visitRoutine_clause(Routine_clauseContext ctx) {
        String name = getFullProcName(ctx.routine_name());
        System.out.println("-> call routine " + name);

        return super.visitRoutine_clause(ctx);
    }

    //    @Override
    //    public Void visitFunction_call(Function_callContext ctx) {
    //        //        String name = getFullProcName(ctx.routine_name());
    //        //        System.out.println("-> call " + name);
    //
    //        return super.visitFunction_call(ctx);
    //    }

    private boolean isFunctionOrProcedureSpec(Id_expressionContext ctx) {
        ParserRuleContext c = ctx;

        while (c != null) {
            if ((c instanceof Function_specContext) || (c instanceof Procedure_specContext)) {
                return true;
            } else if (c instanceof StatementContext) {
                return false;
            }
            c = c.getParent();
        }
        return false;
    }

    // здесь нужно отловить вызов функции
    // она должна быть или определена выше, или должен быть указан пакет, или это глобальная функция
    @Override
    public Void visitId_expression(Id_expressionContext ctx) {
        if (visitMode == AllMethodVistFlag.SECOND_PASS) {
            String name = getFullProcName(ctx);
            //logContext(ctx);

            // если в хеше есть определение, то это может быть вызов функции
            ExpressionType type = idMap.get(name);
            //System.out.println("  // " + name + " type " + type);

            if ((type == ExpressionType.FUNCTION) || (type == ExpressionType.PROCEDURE)) {
                // вызов функции, которая попадалась ранее
                if (!isFunctionOrProcedureSpec(ctx)) {
                   // System.out.printf("\t->> call [%s]\n", name);
                }
            } else if (type == ExpressionType.PACKAGE) {
                System.out.printf("->> package [%s]\n", name);
            } else {
                // при спуске по дереву, для процедур и функций заходит сюда, но это не требуется
                if (!isFunctionOrProcedureSpec(ctx)) {
                   // System.out.printf("- found new type [%s]", name + " [" + ctx.getClass().toString() + "]");
                }
            }
        }
        return super.visitId_expression(ctx);
    }
    
     
    /**
     * get the insert table Name
     */
    @Override public Void visitInsert_into_clause(PlSqlParser.Insert_into_clauseContext ctx) { 
    	
    	 if (visitMode == AllMethodVistFlag.SECOND_PASS) 
    	{
    		 PlSqlParser.Insert_into_clauseContext c = ctx;
    		 
    		 System.out.println(c.general_table_ref().getText());
    		 
    		 this.currentTable = c.general_table_ref().getText();
    		 
    	}
    	
    	
    	return visitChildren(ctx);
    }
    /**
     * Get the node and create a graph
     */
    @Override public Void visitSelect_statement(PlSqlParser.Select_statementContext ctx) {
    	
    	
    	if(this.currentTable  == null)
    	{
    		return visitChildren(ctx); 
    	}
    	if (visitMode == AllMethodVistFlag.SECOND_PASS) 
    	{
    		
    		PlSqlParser.Select_statementContext c  = ctx;
    		
    		System.out.println(ctx.getClass() + "parent->"+ctx.getParent().getClass() 
    				+"children->"+ctx.getChild(0).getClass());
    		
    		System.out.println("Got it ");
    		
    		String result = PlSqlUtils.getOriginalText(input, c);
    		
    		Statement smt;
    		
    		
    		try {
    			
				smt = CCJSqlParserUtil.parse(result);
				
				Select selectStateMent = (Select) smt;
				
				GraphViz gv = new GraphViz();
			   // gv.addln(gv.start_graph());
				gv.addStart();
			    
			    
			    visitTheSelectOnce(selectStateMent.getSelectBody(),this.currentTable,gv);
			    
				
			    gv.addEnd();
			    
			   System.out.println(gv.getDotSource());
				
			   // System.out.println(gv.getPlainDotSource());
				
			    
			    String type = "gif";
			    File out = new File("d:\\dot\\out\\" +  this.currentTable); 
			    gv.writeGraphToFile( gv.getGraph( gv.getDotSource(), type ), out );
				
			    tableAndGraph.put(currentTable, gv);
			    
			    
			    this.currentTable = null;
				
				
			} catch (JSQLParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	return visitChildren(ctx); 
    	
    
    
    }
    
    public void visitTheSelectOnce(SelectBody selectBody, String parent,GraphViz gv)
    {
    	
    	
    	String prefix = this.currentTable+"_";
    	if(selectBody instanceof SetOperationList)
    	{
    		SetOperationList sot = (SetOperationList)selectBody;
    		List<SelectBody> selectBodyList = sot.getSelects();
    		
    		for(int i =0;i<selectBodyList.size();i++)
    		{
    			SelectBody sb  = selectBodyList.get(i);
    			System.out.println(parent+"->>>>"+"xxxxx");
    			
    			visitTheSelectOnce(sb,parent,gv);
    			
    		}
    		return;
    		
    	}
    	
    	PlainSelect plainSelect = (PlainSelect)selectBody;
    	
    	//fromItem
    	
    	FromItem fromItem = plainSelect.getFromItem();
    	
    	if(fromItem instanceof SubSelect)
    	{
    		
    		SubSelect subSelect = (SubSelect) fromItem;
    		
    		System.out.println(parent+"->>>>>"+subSelect.getAlias().getName());
    		
    		//graph it 1
    		//gv.addln(parent + " ->" + subSelect.getAlias().getName() + " ;");
    		gv.addln(prefix+parent + " ->" + prefix+subSelect.getAlias().getName() + " ;");
    		
    		
    		
    		
    		if(null != subSelect.getSelectBody())
    		{
    			visitTheSelectOnce(subSelect.getSelectBody(),subSelect.getAlias().getName(),gv);
    		}
    	}
    	
    	
    	//joins
    	List<Join> joins = plainSelect.getJoins();
    	
    	if(null != joins)
    	{
    		
    		for(int i=0;i<joins.size();i++)
    		{
    			
    			Join join = joins.get(i);
    			
    			FromItem joinItem = join.getRightItem();
    			
    			if(joinItem instanceof Table)
    			{
    				Table joinTable = (Table)joinItem;
    				
    				System.out.println("join[i]="+joinTable.getFullyQualifiedName());
    				
    				//System.out.println(parent+"->>>>>"+joinTable.getAlias().getName());
    				System.out.println(parent+"->>>>>"+joinTable.getAlias().getName());
    				
    				
    				
    				//gv.ad
    				
    				//graph it 2
    	    		//gv.addln(parent + " ->" + joinTable.getFullyQualifiedName()+"_alias_" +joinTable.getAlias().getName() +";");
    	    		gv.addln(prefix+parent + " ->" + prefix+joinTable.getFullyQualifiedName()+"_alias_" +joinTable.getAlias().getName() +";");
    	    		gv.add(prefix+parent+"[label="+parent+"]");
					gv.add(prefix + joinTable.getFullyQualifiedName() + "_alias_" + joinTable.getAlias().getName()
							+ "[label=" + joinTable.getFullyQualifiedName() + "_alias_" + joinTable.getAlias().getName()
							+ "]");
	
					if (tableAndGraph.get(joinTable.getFullyQualifiedName()) != null) {
					
						String subgraph =  "cluster_"+joinTable.getFullyQualifiedName();
		    			gv.addln("subgraph "+subgraph+"{" +tableAndGraph.get(joinTable.getFullyQualifiedName()).getPlainDotSource()+"}");
		    			
		    			gv.addln(prefix+joinTable.getFullyQualifiedName() + "_alias_" + joinTable.getAlias().getName() + "->"
								+joinTable.getFullyQualifiedName()+"_"+joinTable.getFullyQualifiedName()+"[lhead="+subgraph+"]");
						
						
					}
    			
    			
    			}
    			
    			if(joinItem instanceof SubSelect)
    			{
    				
    				System.out.println("join[i]="+joinItem.getAlias().getName());
    				
    				//graph it 3
    	    		//gv.addln(parent + " ->" + joinItem.getAlias().getName() + " ;");
    	    		gv.addln(prefix+parent + " ->" +prefix+ joinItem.getAlias().getName() + " ;");
    	    		
    	    		gv.add(prefix+parent+"[label="+parent+"]");
    	    		gv.add(prefix+ joinItem.getAlias().getName()+"[label="+joinItem.getAlias().getName()+"]");
    	    		
    	    		
    	    		SubSelect joinSubSelect =  (SubSelect) joinItem;
    	    		
    	    		
    	    		if(null != joinSubSelect.getSelectBody())
    	    		{
    	    			visitTheSelectOnce(joinSubSelect.getSelectBody(),joinSubSelect.getAlias().getName(),gv);
    	    		}
    				
    			}
    			
    		}
    		
    		
    	}
    	
    	//table
    	if(fromItem instanceof Table)
    	{
    		Table fromItemTable = (Table) fromItem;
    		
    		
    		System.out.println("join[J]="+fromItemTable.getFullyQualifiedName());
    		
    		System.out.println(parent+"->>>>>"+fromItemTable.getAlias().getName());
    		
    		//graph it 4
    		//gv.addln(parent + " ->" + fromItemTable.getFullyQualifiedName()+"_alias_" +fromItemTable.getAlias().getName() +";");
    		gv.addln(prefix+parent + " ->" +prefix+ fromItemTable.getFullyQualifiedName()+"_alias_" +fromItemTable.getAlias().getName() +";");
        	

    		gv.add(prefix+parent+"[label="+parent+"]");
			gv.add(prefix + fromItemTable.getFullyQualifiedName() + "_alias_" + fromItemTable.getAlias().getName()
					+ "[label=" + fromItemTable.getFullyQualifiedName() + "_alias_" + fromItemTable.getAlias().getName()
					+ "]");

    		if (tableAndGraph.get(fromItemTable.getFullyQualifiedName()) != null) {

    			String subgraph =  "cluster_"+fromItemTable.getFullyQualifiedName();
    			gv.addln("subgraph "+subgraph+"{" +tableAndGraph.get(fromItemTable.getFullyQualifiedName()).getPlainDotSource()+"}");
    			
    			gv.addln(prefix+fromItemTable.getFullyQualifiedName() + "_alias_" + fromItemTable.getAlias().getName() + "->"
						+fromItemTable.getFullyQualifiedName()+"_"+fromItemTable.getFullyQualifiedName()+"[lhead="+subgraph+"]");
				

			}
		
    	
    	}
    	
    	
    	
    }
    
    
    
    
    
}
