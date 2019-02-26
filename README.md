# 参考项目 #
解析SQL: https://github.com/alris/antlr4-oracle

图形化SQL:http://www.graphviz.org/pdf/dotguide.pdf

Python API图形化：https://graphviz.readthedocs.io/en/stable/examples.html#unix-py

graphviz 工具：http://www.graphviz.org/

## 同一方法中表调用关系图形化(已实现） ##
图形化PKG包的调用关系
<pre>
CREATE OR REPLACE PACKAGE BODY scott.pkg1
is
   PROCEDURE proc1
   (
      param1 in number,
	  param2 in varchar2,
	  result out number
   ) is
   begin
	 insert into table1(a1,b1,c1)
	 select h.a1,t.b1,h.c1 from 
	 (select s.a1,t.c1 from tables s , tablet t where s.a1 = t.a1) h,
	 (select x.b1 from tablex x) t 
	 where  t.b1 = h.b1;
	 
   
   end proc1;
   
END;
/
</pre>
![java学习导图](https://github.com/dengdaiyemanren/sqlgraph/blob/master/tests/images/table1.jpg)




## 同一包中的表调用关系图形化 ##

<pre>
PROCEDURE proc2
   (
      param1 in number,
	  param2 in varchar2,
	  result out number
   ) is
   begin
	 insert into table2(a1,b1,c1)
	 select h.a1,t.b1,h.c1 from 
	 (select s.a1,t.c1 from table1 s , tablet t where s.a1 = t.a1) h,
	 (select x.b1 from tablex x) t 
	 where  t.b1 = h.b1;
	 
   
   end proc1;
</pre>
![java学习导图](https://github.com/dengdaiyemanren/sqlgraph/blob/master/tests/images/table2.jpg)


## 跨包的表调用关系图形化  ##
