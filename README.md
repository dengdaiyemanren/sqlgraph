# 参考项目 #
https://github.com/alris/antlr4-oracle

# 目标 #
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



## 同一方法中表调用关系图形化(已实现） ##

## 同一包中的表调用关系图形化 ##

## 跨包的表调用关系图形化  ##
