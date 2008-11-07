package jp.sourceforge.qrcode.geom;

/**
 * This class is used for sampling grid
 * It allows one area to have a different size from another area
 */
public class SamplingGrid
{
	/**
	 * A grid for a single area
	 */
	private class AreaGrid
	{
		protected Line[] xLine;
		protected Line[] yLine;

		public AreaGrid(int width, int height)
		{
			xLine=new Line[width];
			yLine=new Line[height];
		}

		public int getWidth()
		{
			return(xLine.length);
		}

		public int getHeight()
		{
			return(yLine.length);
		}

		public Line getXLine(int x) throws ArrayIndexOutOfBoundsException
		{
			return xLine[x];
		}
		
		public Line getYLine(int y) throws ArrayIndexOutOfBoundsException
		{
			return yLine[y];
		}

		public Line[] getXLines()
		{
			return xLine;
		}
		
		public Line[] getYLines()
		{
			return yLine;
		}

		public void setXLine(int x, Line line)
		{
			xLine[x]=line;
		}

		public void setYLine(int y, Line line)
		{
			yLine[y]=line;
		}
	};

	private AreaGrid[][] grid;

	public SamplingGrid(int sqrtNumArea)
	{
		grid=new AreaGrid[sqrtNumArea][sqrtNumArea];
	}
	
	public void initGrid(int ax,int ay, int width, int height)
	{
		grid[ax][ay]=new AreaGrid(width, height);
	}

	public void setXLine(int ax, int ay, int x, Line line)
	{
		grid[ax][ay].setXLine(x,line);
	}
	
	public void setYLine(int ax, int ay, int y, Line line)
	{
			grid[ax][ay].setYLine(y,line);
	}

	public Line getXLine(int ax, int ay, int x) throws ArrayIndexOutOfBoundsException
	{
		return(grid[ax][ay].getXLine(x));
	}
	
	public Line getYLine(int ax, int ay, int y) throws ArrayIndexOutOfBoundsException
	{
		return(grid[ax][ay].getYLine(y));
	}

	public Line[] getXLines(int ax, int ay)
	{
		return(grid[ax][ay].getXLines());
	}
	
	public Line[] getYLines(int ax, int ay)
	{
		return(grid[ax][ay].getYLines());
	}

	public int getWidth()
	{
		return(grid[0].length);
	}

	public int getHeight()
	{
		return(grid.length);
	}

	public int getWidth(int ax, int ay)
	{
		return(grid[ax][ay].getWidth());
	}

	public int getHeight(int ax, int ay)
	{
		return(grid[ax][ay].getHeight());
	}

	public int getTotalWidth()
	{
		int total=0;
		for(int i=0;i<grid.length;i++)
		{
			total+=grid[i][0].getWidth();
			if(i>0)
				total-=1;
		}
		return total;
	}

	public int getTotalHeight()
	{
		int total=0;
		for(int i=0;i<grid[0].length;i++)
		{
			total+=grid[0][i].getHeight();
			if(i>0)
				total-=1;
		}
		return total;
	}


	public int getX(int ax, int x)
	{
		int total=x;
		for(int i=0;i<ax;i++)
		{
			total+=grid[i][0].getWidth()-1;
		}
		return total;
	}

	public int getY(int ay, int y)
	{
		int total=y;
		for(int i=0;i<ay;i++)
		{
			total+=grid[0][i].getHeight()-1;
		}
		return total;
	}
	
	public void adjust(Point adjust) {
		int dx = adjust.getX(), dy = adjust.getY();
		for (int ay = 0; ay < grid[0].length; ay++) {
			for (int ax = 0; ax < grid.length; ax++) {
				for (int i = 0; i < grid[ax][ay].xLine.length; i++)
					grid[ax][ay].xLine[i].translate(dx, dy);
				for (int j = 0; j < grid[ax][ay].yLine.length; j++)
					grid[ax][ay].yLine[j].translate(dx, dy);
			}
		}
	}

}
