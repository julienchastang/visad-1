//
// EosGrid.java
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
import java.rmi.*;

public class EosGrid extends EosStruct {

  int grid_id;
  int file_id;
  private String grid_name;
 
  DimensionSet  D_Set = null;
   VariableSet  DV_Set;
      ShapeSet  DV_shapeSet;
       GctpMap  gridMap;

  static int DFACC_READ = 1;
  static int D_FIELDS = 4;
  static int N_DIMS = 0;
  static String D_TYPE = "Data Fields";

  EosGrid ( int file_id, String name ) 
  throws HdfeosException 
  {
     super();
     this.file_id = file_id;
     grid_name = name;
     grid_id = Library.Lib.GDattach( file_id, name );

     if ( grid_id < 0 ) 
     {
       throw new HdfeosException("EosGrid cannot attatch Grid: "+name );
     }


/**-  Now make dimensionSet:  - - - - - - - - - - -  -*/

      int[] stringSize = new int[1];
      stringSize[0] = 0;

      DimensionSet D_Set = new DimensionSet();

      int n_dims = Library.Lib.GDnentries( grid_id, N_DIMS, stringSize );
 
      if ( n_dims > 0 ) 
      {

        String[] dimensionList = {"empty"};
        int[] lengths = new int[ n_dims ];

        n_dims = Library.Lib.GDinqdims( grid_id, stringSize[0], dimensionList, lengths );


        if ( n_dims <= 0 ) 
        {
           System.out.println(" error: no dimensions ");
        }

        StringTokenizer listElements = 
                new StringTokenizer( dimensionList[0], ",", false );

        int cnt = 0;

        while ( listElements.hasMoreElements() ) {

          name = (String) listElements.nextElement();
          int len = lengths[cnt];
          NamedDimension obj = new NamedDimension( grid_id, name, len, null );

          D_Set.add( obj );
          cnt++;
        }
      }

      this.D_Set = D_Set;


/**-  Done, now make VariableSets:  - - - - - - - - -*/

       int n_flds = Library.Lib.GDnentries( grid_id, D_FIELDS, stringSize );

       if ( n_flds <= 0 ) 
       {
         throw new HdfeosException(" no data fields  ");
       }

       String[] D_List = {"empty"};

       int[] dumA = new int[ n_flds ];
       int[] dumB = new int[ n_flds ];

       n_flds = Library.Lib.GDinqfields( grid_id, stringSize[0], D_List, dumA, dumB);
      
       if ( n_flds < 0 ) 
       {
          throw new HdfeosException("no data fields in grid struct: "+grid_id);
       }

       this.makeVariables( D_List[0] );


/**-  Done, now make ShapeSet for data fields: - - - - - - - - - */

        DV_shapeSet = new ShapeSet( DV_Set );

/**-  Retrieve map projection type and paramters: - - - - - - -  */

        int[] projcode = new int[1];
        int[] zonecode = new int[1];
        int[] sphrcode = new int[1];
        double[] projparm = new double[16];
        
        int stat = Library.Lib.GDprojinfo( grid_id, projcode, zonecode, sphrcode, projparm );  

          if ( stat < 0 ) {
             System.out.println(" problem: GDprojinfo ");
          }
          else {
             System.out.println(" projcode: "+projcode[0]);
             System.out.println(" zonecode: "+zonecode[0]);
             System.out.println(" sphrcode: "+sphrcode[0]);
 
             for ( int ii = 0; ii < 16; ii++ ) {
               System.out.println(" projparm["+ii+"]: "+projparm[ii] );
             }
          }
 
            int[] xdimsize = new int[1];
            int[] ydimsize = new int[1];
         double[] uprLeft = new double[2];
         double[] lwrRight = new double[2];

         stat = Library.Lib.GDgridinfo( grid_id, xdimsize, ydimsize, uprLeft, lwrRight );

           if ( stat < 0 ) {
              System.out.println(" problem: GDgridinfo ");
           }
           else {
              System.out.println(" uprLeft: "+uprLeft[0]+"  "+uprLeft[1]);
              System.out.println(" lwrRight: "+lwrRight[0]+"  "+lwrRight[1]);
           }

         gridMap = new GctpMap( projcode[0], zonecode[0], sphrcode[0],
                                xdimsize[0], ydimsize[0], projparm, uprLeft, lwrRight ); 

 } /**-  end EosGrid constuctor  - - - - - - - - - - - - -*/


  public int getStructId() {
     return grid_id;
  }

  public GctpMap getMap() {
     return gridMap;
  }

  public ShapeSet getShapeSet() {
    return DV_shapeSet;
  }

  private void makeVariables( String fieldList ) 
               throws HdfeosException
  {

      int[] rank = new int[ 1 ];
      int[] type = new int[ 1 ];
      int[] lengths = new int[ 10 ];

      NamedDimension n_dim;
      int cnt;

      StringTokenizer listElements = new StringTokenizer( fieldList, ",", false );

      VariableSet varSet = new VariableSet();

      while ( listElements.hasMoreElements() ) 
      {

          String field = (String)listElements.nextElement();

          String[] dim_list = {"empty"};

          int stat = Library.Lib.GDfieldinfo( grid_id, field, dim_list, rank, lengths, type );

          if ( stat < 0 ) 
          {
            throw new HdfeosException(" GDfieldinfo, stat < 1 for: "+field );
          }

          StringTokenizer dimListElements = new StringTokenizer( dim_list[0], ",", false );

          Vector dims = new Vector();
          DimensionSet newSet = new DimensionSet();

          cnt = 0;
          while ( dimListElements.hasMoreElements() ) 
          {
              String dimName = (String) dimListElements.nextElement();

              n_dim = D_Set.getByName( dimName );

              if ( n_dim == null ) {
       
                n_dim = new NamedDimension( grid_id, dimName, lengths[cnt], null);
                D_Set.add( n_dim );
              }

              if ( n_dim.isUnlimited() )  {
                n_dim.setLength( lengths[ cnt ] );
              }

              newSet.add( n_dim );
              cnt++;
          }
              newSet.setToFinished();
 

          Variable obj = new Variable(  field, newSet, rank[0], type[0] );
          varSet.add( obj );

      }

          varSet.setToFinished();

          DV_Set = varSet;
   
  }

  public void readData( String field, int[] start, int[] stride, 
                                      int[] edge, int type, float[] data )
    throws HdfeosException
  {
     ReadSwathGrid.readData( this, field, start, stride, edge, type, data);
  }

  public void readData( String field, int[] start, int[] stride, 
                                      int[] edge, int type, double[] data )
    throws HdfeosException
  {
     ReadSwathGrid.readData( this, field, start, stride, edge, type, data);
  }

}
