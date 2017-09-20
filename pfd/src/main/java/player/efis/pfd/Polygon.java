/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package player.efis.pfd;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES20;

/**
 * A two-dimensional line for use as a drawn object in OpenGL ES 2.0.
 */
public class Polygon {


	// This matrix member variable provides a hook to manipulate
	// the coordinates of the objects that use this vertex shader

	private final String vertexShaderCode =
			"uniform mat4 uMVPMatrix;" +
					"attribute vec4 vPosition;" + 
					"void main() {" +
					"  gl_Position = uMVPMatrix * vPosition;" + 
					"}"; // the matrix must be included as a modifier of gl_Position

	private final String fragmentShaderCode = 
			"precision mediump float;" + 
					"uniform vec4 vColor;" + 
					"void main() {"	+ 
					"  gl_FragColor = vColor;" + 
					"}";

	private final FloatBuffer mVertexBuffer;
	private final ShortBuffer drawListBuffer;
	private final  int mProgram;
	private int mPositionHandle;
	private int mColorHandle;
	private int mMVPMatrixHandle;

	// number of coordinates per vertex in this array
	static final int COORDS_PER_VERTEX = 3;
	//static float LineCoords[] = { 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f };
	static float LineCoords[] = new float[300];
	static float LineWidth = 1.0f;
	private final short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

	//private final int VertexCount = LineCoords.length / COORDS_PER_VERTEX;
	int VertexCount; // = LineCoords.length / COORDS_PER_VERTEX;
	private final int VertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per
	// vertex

	// Set color with red, green, blue and alpha (opacity) values
	float color[] = { 0.0f, 0.0f, 0.0f, 1.0f };

	/**
	 * Sets up the drawing object data for use in an OpenGL ES context.
	 */
	public Polygon() {
		// initialize vertex byte buffer for shape coordinates
		ByteBuffer bb = ByteBuffer.allocateDirect(LineCoords.length * 4); // (number of coordinate values * 4 bytes per float)
		// use the device hardware's native byte order
		bb.order(ByteOrder.nativeOrder());

		// create a floating point buffer from the ByteBuffer
		mVertexBuffer = bb.asFloatBuffer();
		// add the coordinates to the FloatBuffer
		mVertexBuffer.put(LineCoords);
		// set the buffer to read the first coordinate
		mVertexBuffer.position(0);
		ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2); // (# of coordinate values * 2 bytes per short)
		dlb.order(ByteOrder.nativeOrder());
		drawListBuffer = dlb.asShortBuffer();
		drawListBuffer.put(drawOrder);
		drawListBuffer.position(0); 

		// prepare shaders and OpenGL program
		int vertexShader = EFISRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
		int fragmentShader = EFISRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

		mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
		GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader  to program
		GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment  shader to program
		GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program  executables

	}

	public void SetVerts(float[] verts) 
	{
		/*for (int i = 0; i < VertexCount * COORDS_PER_VERTEX; i++) {
			LineCoords[i] = verts[i];
		}*/
        System.arraycopy(verts, 0, LineCoords, 0, VertexCount * COORDS_PER_VERTEX); // this is faster?

		mVertexBuffer.put(LineCoords);
		// set the buffer to read the first coordinate
		mVertexBuffer.position(0);
	}

	public void SetColor(float red, float green, float blue, float alpha) 
	{
		color[0] = red;
		color[1] = green;
		color[2] = blue;
		color[3] = alpha;
	}

	public void SetWidth(float width) 
	{
		LineWidth = width;
	}

	/**
	 * Encapsulates the OpenGL ES instructions for drawing this shape.
	 *
	 * @param mvpMatrix - The Model View Project matrix in which to draw
	 * this shape.
	 */
	public void draw(float[] mvpMatrix) 
	{
		// Add program to OpenGL ES environment
		GLES20.glUseProgram(mProgram);

		// get handle to vertex shader's vPosition member
		mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

		// Enable a handle to the triangle vertices
		GLES20.glEnableVertexAttribArray(mPositionHandle);

		// Prepare the line coordinate data
		GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VertexStride, mVertexBuffer);

		// get handle to fragment shader's vColor member
		mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

		// Set color for drawing the line
		GLES20.glUniform4fv(mColorHandle, 1, color, 0);

		GLES20.glLineWidth(LineWidth);

		// get handle to shape's transformation matrix
		mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
		//EFISRenderer.checkGlError("glGetUniformLocation");

		// Apply the projection and view transformation
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
		//EFISRenderer.checkGlError("glUniformMatrix4fv");

		// Draw the line
		//GLES20.glDrawArrays(GLES20.GL_LINES, 0, VertexCount); 
		//GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, VertexCount);

		// Draw the square
		//GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
		//GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, VertexCount);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, VertexCount);


		// Disable vertex array
		GLES20.glDisableVertexAttribArray(mPositionHandle);
	}
}
