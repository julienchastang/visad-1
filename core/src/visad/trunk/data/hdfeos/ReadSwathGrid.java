//
// eos.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 1998 Bill Hibbard, Curtis Rueden, Tom
Rink and Dave Glowacki.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 1, or (at your option)
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License in file NOTICE for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/

package visad.data.hdfeos;

import java.util.*;
import java.lang.*;
import visad.*;

public class ReadSwathGrid
{

   final static int FLOAT = 5;
   final static int DOUBLE = 6;
   final static int INT = 24;
   final static int SHORT = 22;


   public static void readData(  EosStruct struct, 
                                    String f_name,
                                      int[] start,
                                     int[] stride,
                                       int[] edge,
                                     int num_type,
                                   float[] f_data ) 
   throws HdfeosException
   {

      int status = 0;
      int jj;
      int n_values = f_data.length;
         
      int struct_id = struct.getStructId();

      if ( num_type == FLOAT ) 
      {
         System.out.println("f_name: "+f_name);
         System.out.println("start_len"+start.length);
         System.out.println( start[0]+" "+start[1]+" "+start[2]);
         System.out.println("edge_len"+edge.length);
         System.out.println( edge[0]+" "+edge[1]+" "+edge[2]);
         if ( struct instanceof EosGrid ) 
         {
           status = Library.Lib.GDreadfield( struct_id, f_name, start, stride, edge, f_data );
           System.out.println("read status: "+status);
         }
         else if ( struct instanceof EosSwath )
         {
            status = Library.Lib.SWreadfield( struct_id, f_name, start, stride, edge, f_data );
         }
     
      }
      else if ( num_type == DOUBLE ) 
      {
         double[] d_data = new double[ n_values ];

         if( struct instanceof EosGrid ) 
         {
           status = Library.Lib.GDreadfield( struct_id, f_name, start, stride, edge, d_data );
         }
         else if ( struct instanceof EosSwath )
         {
           status = Library.Lib.SWreadfield( struct_id, f_name, start, stride, edge, d_data );
         }

         for ( jj = 0; jj < n_values; jj++ ) 
         {
           f_data[jj] = (float)d_data[jj];
         }
           d_data = null;
      }
      else if ( num_type == INT ) 
      {
        int[] i_data = new int[ n_values ];
        
        if ( struct instanceof EosGrid )
        {
          status = Library.Lib.GDreadfield( struct_id, f_name, start, stride, edge, i_data );
        }
        else if ( struct instanceof EosSwath )
        {
          status = Library.Lib.SWreadfield( struct_id, f_name, start, stride, edge, i_data );
        }

        for ( jj = 0; jj < n_values; jj++ ) 
        {
          f_data[jj] = (float)i_data[jj];
        }
          i_data = null;
      }
      else if ( num_type == SHORT )
      {
        short[] s_data = new short[ n_values ];
 
        if ( struct instanceof EosGrid ) 
        {
          status = Library.Lib.SWreadfield( struct_id, f_name, start, stride, edge, s_data );
        }
        else if ( struct instanceof EosSwath )
        {
          status = Library.Lib.SWreadfield( struct_id, f_name, start, stride, edge, s_data );
        }

        for ( jj = 0; jj < n_values; jj++ ) 
        {
          f_data[jj] = (float)s_data[jj];
        }
          s_data = null;
      }
      else 
      {
         throw new HdfeosException(" number type not recognized: "+num_type );
      }

      if ( status < 0 ) 
      {
         throw new HdfeosException(" SWreadfield, status: "+status );
      }

      return;
   }

   public static void readData( EosStruct struct,
                                   String f_name,
                                     int[] start,
                                    int[] stride,
                                      int[] edge,
                                    int num_type,
                                 double[] d_data )
   throws HdfeosException
   {

     int jj;
     int status = 0;
     int n_values = d_data.length;

     int struct_id = struct.getStructId();

         if ( num_type == FLOAT ) 
         {
             float[] f_data = new float[ n_values ];
 
             if ( struct instanceof EosGrid )
             {
               status = Library.Lib.GDreadfield( struct_id, f_name, start, stride, edge, f_data );
             }
             else if ( struct instanceof EosSwath )
             {
               status = Library.Lib.SWreadfield( struct_id, f_name, start, stride, edge, f_data );
             }

             for ( jj = 0; jj < n_values; jj++ ) 
             {
               d_data[jj] = (double) f_data[jj];
             }
             f_data = null;
          }
          else if ( num_type == DOUBLE ) 
          {
             if ( struct instanceof EosGrid ) 
             {
               status = Library.Lib.GDreadfield( struct_id, f_name, start, stride, edge, d_data );
             }
             else if ( struct instanceof EosSwath )
             {
               status = Library.Lib.SWreadfield( struct_id, f_name, start, stride, edge, d_data );
             }
          }
          else if ( num_type == INT ) 
          {
             int[] i_data = new int[ n_values ];

             if ( struct instanceof EosGrid )
             {
               status = Library.Lib.GDreadfield( struct_id, f_name, start, stride, edge, i_data );
             }
             else if ( struct instanceof EosSwath )
             {
               status = Library.Lib.SWreadfield( struct_id, f_name, start, stride, edge, i_data );
             }

             for ( jj = 0; jj < n_values; jj++ ) 
             {
               d_data[jj] = (double)i_data[jj];
             }
             i_data = null;
          }
          else if ( num_type == SHORT )
          {
             short[] s_data = new short[ n_values ];

             if ( struct instanceof EosGrid )
             {
               status = Library.Lib.GDreadfield( struct_id, f_name, start, stride, edge, s_data );
             }
             else if ( struct instanceof EosSwath )
             {
               status = Library.Lib.SWreadfield( struct_id, f_name, start, stride, edge, s_data );
             }

             for ( jj = 0; jj < n_values; jj++ ) 
             {
               d_data[jj] = (double)s_data[jj];
             }
             s_data = null;
          }
          else 
          {
             throw new HdfeosException(" number type not recognized: "+num_type );
          }

         if ( status < 0 )
         {
           throw new HdfeosException(" SWreadfield, status: "+status );
         }
   
         return;
   }
}
