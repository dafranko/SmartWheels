#include "tm4c123gh6pm.h"
#include "stdint.h"
#include "stdlib.h"
#include "UART0.h"
#include "UART1.h"
#include "UART2.h"
#include "TIMER3A.h"
#include "TIMER2A.h"
#include "PortC_pwm_init.h"
#include "PortC_6_7_Init.h"
#include "PortB_Direction_Init.h"
#include "PortF_LED_Init.h"
#include "PORTE.h"
#include "PLL.h"
#include <math.h>
void PORTE_Init(void);//remember to modularize!!!!!!!!!!                         
int power(int x, unsigned int y);//function created to get power of var, did not need it
unsigned int hc_sr04_conv(int e);//mean for converting but is just used to shrink input value from hc-sr04
void Robot_Controller(unsigned char character);

#define CR 0x0d
#define BS 0x08


unsigned int inc = 0,e0 = 0,e1 = 0,e2 = 0,e3 = 0;
int countr = 0,countl = 0;//counts the number of slots read from the encoder, 
int rpm = 60;int rpmr,rpml = 0;
int speed_reductionl,speed_reductionr = 20000;
int dinputl,dinputr,lastinputl,lastinputr = 0;
int proportionall=0, errorl=0,proportionalr=0, errorr=0,integrall = 0, integralr = 0, error_suml, error_sumr; 
int derivativel = 0, prev_errorl = 0,derivativer = 0, prev_errorr = 0;
double kp = 2, ki = 5, kd = 9;


int main(void){ char character;
 	PLL_Init();
	PORTF_INIT();
	UART0_INIT();
	UART1_INIT();
	PORTE_Init();    
	PortB_Direction_Init();
	PORTC_PWM0_GEN3_AB_Init();
	PortC_Direction_Init();
	timer3_Init();
	timer2_Init();	
	TIMER2_CTL_R |= 0x00000001;


  while(1){
			character = UART2_InChar();
			if(character == '1'){
															PC4_PWM(39998);
															PC5_PWM(39998);					
			}
			if(character == '2'){
						if(inc == 7900){GPIO_PORTC_DATA_R |= 0x80;}
						if(inc == 7910){GPIO_PORTC_DATA_R &= ~0x80;}
						if(inc == 30000){GPIO_PORTC_DATA_R |= 0x40;}
						if(inc == 30010){GPIO_PORTC_DATA_R &= ~0x40;}															
			}
			if(character == '3'){
				 Robot_Controller(character);
			}

	}
}










void Robot_Controller(unsigned char character){
		
	

	if (character == 'F')
			{
				//rpm =  120;		
				GPIO_PORTF_DATA_R = (GPIO_PORTF_DATA_R& ~0x0E) |BLUE;
				//GPIO_PORTC_DATA_R = (GPIO_PORTC_DATA_R & ~0xC0) | 0x80;
				GPIO_PORTB_DATA_R = (GPIO_PORTB_DATA_R & ~0xF0) | 0xA0;
				PC4_PWM(39998 - speed_reductionl);
				PC5_PWM(39998 - speed_reductionr);
			}
		else if (character == 'B')
			{
				//rpm =  120;
				GPIO_PORTF_DATA_R = (GPIO_PORTF_DATA_R& ~0x0E) |RED;
				//GPIO_PORTC_DATA_R = (GPIO_PORTC_DATA_R & ~0xC0) | 0x40;
				GPIO_PORTB_DATA_R = (GPIO_PORTB_DATA_R & ~0xF0) | 0x50;
				PC4_PWM(39998 - speed_reductionl);
				PC5_PWM(39998 - speed_reductionr);

			}
		else if (character == 'L')
			{
				//rpm =  90;
				GPIO_PORTF_DATA_R = (GPIO_PORTF_DATA_R& ~0x0E) |GREEN;
				//GPIO_PORTC_DATA_R = (GPIO_PORTC_DATA_R & ~0xC0) | 0x80;
				GPIO_PORTB_DATA_R = (GPIO_PORTB_DATA_R & ~0xF0) | 0xA0;
				PC4_PWM(39998);
				PC5_PWM(39998 - speed_reductionr);//right
			}
		else if (character == 'R')
			{
				//rpm =  90;
				GPIO_PORTF_DATA_R = (GPIO_PORTF_DATA_R& ~0x0E) |YELLOW;
				//GPIO_PORTC_DATA_R = (GPIO_PORTC_DATA_R & ~0xC0) | 0x80;
				GPIO_PORTB_DATA_R = (GPIO_PORTB_DATA_R & ~0xF0) | 0xA0;
				PC4_PWM(39998 - speed_reductionl);//left
				PC5_PWM(39998);
			}
		else if(character == 'S')
			{
				//rpm =  0;
				GPIO_PORTF_DATA_R = (GPIO_PORTF_DATA_R& ~0x0E) |PINK;
				//GPIO_PORTC_DATA_R = (GPIO_PORTC_DATA_R & ~0xC0) | 0x80;
				GPIO_PORTB_DATA_R = (GPIO_PORTB_DATA_R & ~0xF0) | 0xA0;
				PC4_PWM(39998);
				PC5_PWM(39998);
			}
		else
		{
			//rpm =  0;
			speed_reductionl = 0;
			speed_reductionr = 0;
			GPIO_PORTF_DATA_R = (GPIO_PORTF_DATA_R& ~0x0E) |SKY_BLUE;
			//GPIO_PORTC_DATA_R = (GPIO_PORTC_DATA_R & ~0xC0) | 0x80;
			GPIO_PORTB_DATA_R = (GPIO_PORTB_DATA_R & ~0xF0) | 0xA0;
			PC4_PWM(39998);
			PC5_PWM(39998);
		}
}



void self_pid(){
	
	
	errorl = rpm - rpml;
	error_suml += errorl;
	dinputl = rpml - lastinputl;
	speed_reductionl = kp*errorl + ki*error_suml + kd*dinputl;
	lastinputl = rpml;
	if(speed_reductionl < 10000){
		speed_reductionl = 10000;
	}
	else if(speed_reductionl > 30000){
		speed_reductionl = 30000;
	}
	
	
	errorr = rpm - rpmr;
	error_sumr += errorr;
	dinputr = rpmr - lastinputr;
	speed_reductionr = kp*errorr + ki*error_sumr + kd*dinputr;
	lastinputr = rpml;
	if(speed_reductionr < 10000){
		speed_reductionr = 10000;
	}
	else if(speed_reductionr > 30000){
		speed_reductionr = 30000;
	}
	
	
	/*
	prev_errorl = errorl;
	errorl = rpml - rpm; 
	error_suml += errorl;
	proportionall = kp * errorl;
	integrall = ki * error_suml;
	derivativel = kd * (errorl - prev_errorl);
	speed_reductionl = proportionall + integrall + derivativel;
	
	prev_errorr = errorr;
	errorr = rpmr - rpm;  
	error_sumr += errorr;
	proportionalr = kp * errorr;
	integralr = ki * error_sumr;
	derivativer = kd * (errorr - prev_errorr);
	speed_reductionr = proportionalr + integralr + derivativer;
	*/
	
}


void GPIOPortE_Handler(void){
	
	if(GPIO_PORTE_RIS_R&0x01){  
    GPIO_PORTE_ICR_R |= 0x01;  
		e0 = ((inc-7910)*343)/2000;
	}
		
  if(GPIO_PORTE_RIS_R&0x02){ 
    GPIO_PORTE_ICR_R |= 0x02;	
		e1 = ((inc-7910)*343)/2000;
		
  }	
	
	
  if(GPIO_PORTE_RIS_R&0x04){ 
    GPIO_PORTE_ICR_R |= 0x04;		
		//e2 = (int)(0.0343 * ((inc)/2000));
		e2 = (int)((inc-30010)*343)/2000;

  }	
	if(GPIO_PORTE_RIS_R&0x08){ 
    GPIO_PORTE_ICR_R |= 0x08;		
		e3 = ((inc-7910)*343)/2000;

	}
}










void Timer3A_Handler(void){//100 milisecond
	volatile uint32_t readback;	

	e0=hc_sr04_conv(e0);
	e1=hc_sr04_conv(e1);
	e2=hc_sr04_conv(e2)/2;
	e3=hc_sr04_conv(e3);

		if(e2 <= 4000){
															PC4_PWM(39998);
															PC5_PWM(39998);	
							 }
		else{
					if(e1<35000){//if middle sensor is triggered then stop rotation
										 GPIO_PORTF_DATA_R |= GREEN;
										 GPIO_PORTB_DATA_R = (GPIO_PORTB_DATA_R & ~0xF0) | 0xA0;
											PC4_PWM(39998 - 39000);
											PC5_PWM(39998 - 39000*.85);
										 
				
						}
						else{
						
									GPIO_PORTF_DATA_R &= ~GREEN;
									if(e0<35000){//rotate counter clockwise
															 GPIO_PORTF_DATA_R |= RED;
															 GPIO_PORTB_DATA_R = (GPIO_PORTB_DATA_R & ~0xF0) | 0x60;
															 PC4_PWM(39998 - 39000);
															 PC5_PWM(39998 - 39000*.85);
									}
									else{				GPIO_PORTF_DATA_R &= ~RED;}
									if(e3<35000){//rotate clockwise
															 GPIO_PORTF_DATA_R |= BLUE;
															 GPIO_PORTB_DATA_R = (GPIO_PORTB_DATA_R & ~0xF0) | 0x90;
															 PC4_PWM(39998 - 39000);
															 PC5_PWM(39998 - 39000*.85);
										}
									else{GPIO_PORTF_DATA_R &= ~BLUE;}
								}

						if((GPIO_PORTF_DATA_R&0X0E) == DARK){
																			 PC4_PWM(39998);
																			 PC5_PWM(39998);
						}
					}
	/*if((GPIO_PORTF_DATA_R&0X0E) == DARK){
																			 PC4_PWM(39998);
																			 PC5_PWM(39998);
	}
	*/
	
	
	
		UART0_transmit_String("PORTE0: ");		
		UART0_transmit_Integer(e0);
		UART0_transmit_String("\t");
		
		UART0_transmit_String("PORTE1: ");		
		UART0_transmit_Integer(e1);
		UART0_transmit_String("\t");

		UART0_transmit_String("PORTE2: ");		
		UART0_transmit_Integer(e2);
		UART0_transmit_String("\t");
	
		UART0_transmit_String("PORTE3: ");		
		UART0_transmit_Integer(e3);
		UART0_transmit_String("\t");
	
		UART0_transmit_String("\r\n");
	
	
	
	
		e0=e1=e2=e3=0;
	
	
	///////////////////////////////
	GPIO_PORTC_DATA_R &= ~0xC0;	
	inc = 0;	
	UART1_OutChar('A');
	TIMER3_ICR_R = 0x01;			// clear interrupt flag	
	readback = TIMER3_ICR_R;
	
}	




	
void Timer2A_Handler(void){//1 microseconds
	inc += 1;
	TIMER2_ICR_R = 0x01;			// clear interrupt flag	
}	



void PORTE_Init(void){                          
  SYSCTL_RCGC2_R |= 0x00000010; // (a) activate clock for port E
  GPIO_PORTE_DIR_R &= ~0x0F;    // (c) make PF4 in (built-in button)
  GPIO_PORTE_AFSEL_R &= ~0x0F;  //     disable alt funct on PF4
  GPIO_PORTE_DEN_R |= 0x0F;     //     enable digital I/O on PF4   
  GPIO_PORTE_PCTL_R &= ~0x0000FFFF; // configure PF4 as GPIO
  GPIO_PORTE_AMSEL_R = 0;       //     disable analog functionality on PF
  GPIO_PORTE_PUR_R |= 0x0F;     //     enable weak pull-up on PF4
  GPIO_PORTE_IS_R &= ~0x0F;     // (d) PF4 is edge-sensitive
  GPIO_PORTE_IBE_R &= ~0x0F;    //     PF4 is not both edges
  GPIO_PORTE_IEV_R &= ~0x0F;    //     PF4 falling edge event
  GPIO_PORTE_ICR_R = 0x0F;      // (e) clear flag4
  GPIO_PORTE_IM_R |= 0x0F;      // (f) arm interrupt on PF4
  NVIC_PRI1_R = (NVIC_PRI1_R&0xFF00FFFF)|0x00A00000; // (g) priority 5
  NVIC_EN0_R |= 0x00000010;      // (h) enable interrupt 4 in NVIC
}
/*
*/

unsigned int hc_sr04_conv(int e){
	
	unsigned int y;
	y = 77850*e - 6237000;

	
	return y/1000;
	
	
	
}



