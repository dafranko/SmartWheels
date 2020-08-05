
$fn = 64;

module solidBoard()
{
    pcbthickness = 1.65;
    cube([43,20,pcbthickness],false);
    
    {
     translate([10,10,pcbthickness])
     cylinder(h=15-pcbthickness,d=16);
     
     translate([32,10,pcbthickness])
     cylinder(h=15-pcbthickness,d=16);  
    } 
 }
    
module mountingHoles()
 {
    translate([2.5,2.5,-0.5])
    cylinder(h=3,d=3);
     
    translate([2.5+38,2.5,-0.5])
    cylinder(h=3,d=3);
     
 }
 
 module connectorPins()
 {
    rotate([90,0,0])
    translate([43*0.5-2.54*2,-1.5,-2.5])
    {
     cube([0.66,0.66,10],false);
     
     translate([2.54,0])
     cube([0.66,0.66,10],false);  
        
     translate([2.54*2,0])
     cube([0.66,0.66,10],false);
        
     translate([2.54*3,0])
     cube([0.66,0.66,10],false);
        
     translate([-1.2,-1,0])
     cube([2.54*4,2.5,2.5]);
    };
     
 }
 
 module ultrasonicSensor()
 {
    difference()
    {
      union()
      {
        solidBoard();
        connectorPins();
          
      } 
     
      union()
      {
        mountingHoles();  

      } 

     }
     
 }
 
 module sensorAssembly()
 {
    translate([43*0.5,0,0])
    rotate([90,0,180])
    ultrasonicSensor();
   
 }
 

 /*
 translate([0,35,0])
 sensorAssembly();
 
 translate([80,0,0])
 rotate([0,0,-65])
 sensorAssembly();

 translate([-80,0,0])
 rotate([0,0,65])
 sensorAssembly();

 translate([40,25,0])
 rotate([0,0,-35])
 sensorAssembly();

 translate([-40,25,0])
 rotate([0,0,35])
 sensorAssembly(); 
 */
rotate([90,0,180])
ultrasonicSensor();
