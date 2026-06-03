/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rnd.blockly;

/**
 *
 * @author saliya
 */
/**
 * Simple callback interface replacing Consumer<Block>
 * for Java 6/7 source-level compatibility.
 */
public interface BlockAddListener {

    void onBlockAdded(Block block);
}
