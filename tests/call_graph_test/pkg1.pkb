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
   
   PROCEDURE proc2
   (
      param1 in number,
	  param2 in varchar2,
	  result out number
   ) is
   begin
   
   
   	insert into table1(a1,b1,c1)
	 select h.a1,t.b1,h.c1 from 
	 (select s.a1,t.c1 from tablesAA s , tabletAA t where s.a1 = t.a1) h,
	 (select x.b1 from tablex x) t 
	 where  t.b1 = h.b1;
	 
	 
	 
	 insert into table2(a1,b1,c1)
	 select h.a1,t.b1,h.c1 from 
	 (select s.a1,t.c1 from table1 s , tablet t where s.a1 = t.a1) h,
	 (select x.b1 from tablex x) t 
	 where  t.b1 = h.b1;
	 

	 
   
   end proc1;
   
   
   
END;
/