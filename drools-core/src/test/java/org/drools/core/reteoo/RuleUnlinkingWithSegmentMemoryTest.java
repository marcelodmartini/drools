package org.drools.core.reteoo;

import static org.junit.Assert.*;

import java.util.List;

import org.drools.core.RuleBaseConfiguration;
import org.drools.core.RuleBaseFactory;
import org.drools.core.base.ClassObjectType;
import org.drools.core.common.AbstractWorkingMemory;
import org.drools.core.common.DefaultFactHandle;
import org.drools.core.common.EmptyBetaConstraints;
import org.drools.core.common.NetworkNode;
import org.drools.core.common.PropagationContextFactory;
import org.drools.core.phreak.SegmentUtilities;
import org.drools.core.reteoo.builder.BuildContext;
import org.drools.core.rule.GroupElement;
import org.drools.core.rule.Rule;
import org.drools.core.rule.GroupElement.Type;
import org.drools.core.spi.PropagationContext;
import org.junit.Test;
import org.kie.api.KieBaseConfiguration;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.builder.conf.RuleEngineOption;

public class RuleUnlinkingWithSegmentMemoryTest {
    ReteooRuleBase     ruleBase;
    BuildContext       buildContext;
    PropagationContext context;

    LeftInputAdapterNode lian;
    BetaNode           n1;
    BetaNode           n2;
    BetaNode           n3;
    BetaNode           n4;
    BetaNode           n5;
    BetaNode           n6;
    BetaNode           n7;
    BetaNode           n8;
    BetaNode           n9;
    BetaNode           n10;
    
    RuleTerminalNode   rtn1;
    RuleTerminalNode   rtn2;
    RuleTerminalNode   rtn3;
    
    Rule               rule1;
    Rule               rule2;
    Rule               rule3;

    static final int   JOIN_NODE             = 0;
    static final int   EXISTS_NODE           = 1;
    static final int   NOT_NODE              = 2;
    static final int   RULE_TERMINAL_NODE    = 3;    
    
    private NetworkNode createNetworkNode(int id,
                                          int type,
                                          LeftTupleSource leftTupleSource,
                                          Rule rule) {
        MockObjectSource mockObjectSource = new MockObjectSource( 8 );

        LeftTupleSink networkNode = null;
        switch ( type ) {
            case JOIN_NODE : {
                networkNode = new JoinNode( id, leftTupleSource, mockObjectSource, new EmptyBetaConstraints(), buildContext );
                break;
            }
            case EXISTS_NODE : {
                networkNode = new ExistsNode( id, leftTupleSource, mockObjectSource, new EmptyBetaConstraints(), buildContext );
                break;
            }
            case NOT_NODE : {
                networkNode = new NotNode( id, leftTupleSource, mockObjectSource, new EmptyBetaConstraints(), buildContext );
                break;
            }   
            case RULE_TERMINAL_NODE : {
                networkNode = new RuleTerminalNode( id, leftTupleSource, rule, new GroupElement( Type.AND ), 0, buildContext);
                break;
            }             
        }

        mockObjectSource.attach();
        if ( NodeTypeEnums.isLeftTupleSource( networkNode ) ) {
            ((LeftTupleSource)networkNode).attach();
        } else {
            ((RuleTerminalNode)networkNode).attach();
        }

        return networkNode;
    }
    
    public void setUp(int type) {
        KieBaseConfiguration kconf = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
        kconf.setOption( RuleEngineOption.PHREAK );
        ruleBase = (ReteooRuleBase) RuleBaseFactory.newRuleBase((RuleBaseConfiguration)kconf);
        
        buildContext = new BuildContext( ruleBase, ruleBase.getReteooBuilder().getIdGenerator() );

        PropagationContextFactory pctxFactory = ruleBase.getConfiguration().getComponentFactory().getPropagationContextFactory();
        context = pctxFactory.createPropagationContext(0, PropagationContext.INSERTION, null, null, null);

        ObjectTypeNode otn = new ObjectTypeNode( 2, null, new ClassObjectType( String.class ), buildContext );
        lian = new LeftInputAdapterNode(3, otn, buildContext );

        n1 = (BetaNode) createNetworkNode( 10, type, lian, null );
        n2 = (BetaNode) createNetworkNode( 11, type, n1, null );
        n3 = (BetaNode) createNetworkNode( 12, type, n2, null );        
        rule1 =  new Rule("rule1");
        rule1.setActivationListener( "agenda" );
        rtn1 = ( RuleTerminalNode ) createNetworkNode( 18, RULE_TERMINAL_NODE, n3, rule1);
        
      
        n4 = (BetaNode) createNetworkNode( 13, type, n3, null );
        n5 = (BetaNode) createNetworkNode( 14, type, n4, null );
        rule2 =  new Rule("rule2");
        rule2.setActivationListener( "agenda" );          
        rtn2 = ( RuleTerminalNode ) createNetworkNode( 19, RULE_TERMINAL_NODE, n5, rule2 );        
        

        n6 = (BetaNode) createNetworkNode( 15, type, n5, null );
        n7 = (BetaNode) createNetworkNode( 16, type, n6, null );
        n8 = (BetaNode) createNetworkNode( 17, type, n7, null );
        rule3 =  new Rule("rule3");
        rule3.setActivationListener( "agenda" ); 
        rtn3 = ( RuleTerminalNode ) createNetworkNode( 20, RULE_TERMINAL_NODE, n8, rule3 );


        lian.addAssociation( rule1, null );
        lian.addAssociation( rule2, null );
        lian.addAssociation( rule3, null );
        n1.addAssociation( rule1, null );
        n1.addAssociation( rule2, null );
        n1.addAssociation( rule3, null );
        n2.addAssociation( rule1, null );
        n2.addAssociation( rule2, null );
        n2.addAssociation( rule3, null );
        n3.addAssociation( rule1, null );
        n3.addAssociation( rule2, null );
        n3.addAssociation( rule3, null );

        n4.addAssociation( rule2, null );
        n4.addAssociation( rule3, null );
        n5.addAssociation( rule2, null );
        n5.addAssociation( rule3, null );
        
        n6.addAssociation( rule3, null );
        n7.addAssociation( rule3, null );
        n8.addAssociation( rule3, null );
    }
    
    @Test
    public void testRuleSegmentsAllLinkedTestMasks() {
        setUp( JOIN_NODE );

        KieBaseConfiguration kconf = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
        kconf.setOption( RuleEngineOption.PHREAK );
        AbstractWorkingMemory wm = new AbstractWorkingMemory( 1, (ReteooRuleBase) RuleBaseFactory.newRuleBase((RuleBaseConfiguration) kconf) );
        
        PathMemory rs = (PathMemory) wm.getNodeMemory( rtn1 );
        assertFalse( rs.isRuleLinked() );
        assertEquals( 1, rs.getAllLinkedMaskTest() );
        
        rs = (PathMemory) wm.getNodeMemory( rtn2 );
        assertFalse( rs.isRuleLinked() );
        assertEquals( 3, rs.getAllLinkedMaskTest() );
        
        rs = (PathMemory) wm.getNodeMemory( rtn3 );
        assertFalse( rs.isRuleLinked() );
        assertEquals( 7, rs.getAllLinkedMaskTest() );
    }   
    
    
    @Test
    public void testSegmentNodeReferencesToSegments() {
        setUp( JOIN_NODE );

        KieBaseConfiguration kconf = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
        kconf.setOption( RuleEngineOption.PHREAK );
        AbstractWorkingMemory wm = new AbstractWorkingMemory( 1, (ReteooRuleBase) RuleBaseFactory.newRuleBase((RuleBaseConfiguration)kconf) );

        BetaMemory bm = null;
        List<PathMemory> list;
        
        PathMemory rtn1Rs = (PathMemory) wm.getNodeMemory( rtn1 );
        PathMemory rtn2Rs = (PathMemory) wm.getNodeMemory( rtn2 );
        PathMemory rtn3Rs = (PathMemory) wm.getNodeMemory( rtn3 );

        // lian
        SegmentUtilities.createSegmentMemory( lian, wm );
        LeftInputAdapterNode.LiaNodeMemory lmem = (LeftInputAdapterNode.LiaNodeMemory) wm.getNodeMemory( lian );
        assertEquals( 1, lmem.getNodePosMaskBit() );

        // n1
        SegmentUtilities.createSegmentMemory( n1, wm );
        bm = (BetaMemory) wm.getNodeMemory( n1 );
        assertEquals( 2, bm.getNodePosMaskBit() );
        assertEquals( 15, bm.getSegmentMemory().getAllLinkedMaskTest() );
        assertEquals( 1, bm.getSegmentMemory().getSegmentPosMaskBit() );        
        list = bm.getSegmentMemory().getPathMemories();
        assertEquals( 3, list.size());
        assertTrue( list.contains( rtn1Rs ) );
        assertTrue( list.contains( rtn2Rs ) );
        assertTrue( list.contains( rtn3Rs ) );           
        
        // n2
        bm = (BetaMemory) wm.getNodeMemory( n2 );
        assertEquals( 4, bm.getNodePosMaskBit() );
        assertEquals( 15, bm.getSegmentMemory().getAllLinkedMaskTest() );
        assertEquals( 1, bm.getSegmentMemory().getSegmentPosMaskBit() );        
        list = bm.getSegmentMemory().getPathMemories();
        assertEquals( 3, list.size());
        assertTrue( list.contains( rtn1Rs ) );
        assertTrue( list.contains( rtn2Rs ) );
        assertTrue( list.contains( rtn3Rs ) );           
       
        // n3
        bm = (BetaMemory) wm.getNodeMemory( n3 );
        assertEquals( 8, bm.getNodePosMaskBit() );
        assertEquals( 15, bm.getSegmentMemory().getAllLinkedMaskTest() );
        assertEquals( 1, bm.getSegmentMemory().getSegmentPosMaskBit() );        
        list = bm.getSegmentMemory().getPathMemories();
        assertEquals( 3, list.size());
        assertTrue( list.contains( rtn1Rs ) );
        assertTrue( list.contains( rtn2Rs ) );
        assertTrue( list.contains( rtn3Rs ) );           
        
        // n4
        SegmentUtilities.createSegmentMemory( n4, wm );
        bm = (BetaMemory) wm.getNodeMemory( n4 );
        assertEquals( 1, bm.getNodePosMaskBit() );
        assertEquals( 3, bm.getSegmentMemory().getAllLinkedMaskTest() );
        assertEquals( 2, bm.getSegmentMemory().getSegmentPosMaskBit() );        
        list = bm.getSegmentMemory().getPathMemories();
        assertEquals( 2, list.size());
        assertTrue( list.contains( rtn2Rs ) );
        assertTrue( list.contains( rtn3Rs ) );        
        
        // n5
        bm = (BetaMemory) wm.getNodeMemory( n5 );
        assertEquals( 2, bm.getNodePosMaskBit() );
        assertEquals( 3, bm.getSegmentMemory().getAllLinkedMaskTest() );
        assertEquals( 2, bm.getSegmentMemory().getSegmentPosMaskBit() );        
        list = bm.getSegmentMemory().getPathMemories();
        assertEquals( 2, list.size());
        assertTrue( list.contains( rtn2Rs ) );
        assertTrue( list.contains( rtn3Rs ) );
        
        // n6
        SegmentUtilities.createSegmentMemory( n6, wm );        
        bm = (BetaMemory) wm.getNodeMemory( n6 );
        assertEquals( 1, bm.getNodePosMaskBit() );
        assertEquals( 7, bm.getSegmentMemory().getAllLinkedMaskTest() );
        assertEquals( 4, bm.getSegmentMemory().getSegmentPosMaskBit() );        
        list = bm.getSegmentMemory().getPathMemories();
        assertEquals( 1, list.size());
        assertTrue( list.contains( rtn3Rs ) );    
        
        // n7
        bm = (BetaMemory) wm.getNodeMemory( n7 );
        assertEquals( 2, bm.getNodePosMaskBit() );
        assertEquals( 7, bm.getSegmentMemory().getAllLinkedMaskTest() );
        assertEquals( 4, bm.getSegmentMemory().getSegmentPosMaskBit() );        
        list = bm.getSegmentMemory().getPathMemories();
        assertEquals( 1, list.size());
        assertTrue( list.contains( rtn3Rs ) );    
        
        // n8
        bm = (BetaMemory) wm.getNodeMemory( n8 );
        assertEquals( 4, bm.getNodePosMaskBit() );
        assertEquals( 7, bm.getSegmentMemory().getAllLinkedMaskTest() );
        assertEquals( 4, bm.getSegmentMemory().getSegmentPosMaskBit() );        
        list = bm.getSegmentMemory().getPathMemories();
        assertEquals( 1, list.size());
        assertTrue( list.contains( rtn3Rs ) );
    }       
    
    @Test
    public void testRuleSegmentLinking() {
        setUp( JOIN_NODE );

        KieBaseConfiguration kconf = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
        kconf.setOption( RuleEngineOption.PHREAK );
        AbstractWorkingMemory wm = new AbstractWorkingMemory( 1, (ReteooRuleBase) RuleBaseFactory.newRuleBase((RuleBaseConfiguration)kconf) );

        BetaMemory bm = null;
        List<PathMemory> list;
        
        PathMemory rtn1Rs = (PathMemory) wm.getNodeMemory( rtn1 );
        PathMemory rtn2Rs = (PathMemory) wm.getNodeMemory( rtn2 );
        PathMemory rtn3Rs = (PathMemory) wm.getNodeMemory( rtn3 );
        
        DefaultFactHandle f1 = (DefaultFactHandle) wm.insert( "test1" );

        lian.assertObject( f1, context, wm );
        n1.assertObject( f1, context, wm );
        n3.assertObject( f1, context, wm );
        n4.assertObject( f1, context, wm );
        n8.assertObject( f1, context, wm );
        
        assertFalse( rtn1Rs.isRuleLinked() );
        assertFalse( rtn2Rs.isRuleLinked() );
        assertFalse( rtn3Rs.isRuleLinked() );
        

        // Link in Rule1
        bm = (BetaMemory) wm.getNodeMemory( n2 );
        assertFalse( bm.getSegmentMemory().isSegmentLinked() );
        
        DefaultFactHandle f2 = (DefaultFactHandle) wm.insert( "test2" );
        n2.assertObject( f2, context, wm );
        assertTrue( bm.getSegmentMemory().isSegmentLinked() );
        
        assertTrue( rtn1Rs.isRuleLinked() );
        assertFalse( rtn2Rs.isRuleLinked() );
        assertFalse( rtn3Rs.isRuleLinked() );        
        
        // Link in Rule2
        bm = (BetaMemory) wm.getNodeMemory( n5 );
        assertFalse( bm.getSegmentMemory().isSegmentLinked() );
        
        n5.assertObject( f1, context, wm );
        assertTrue( bm.getSegmentMemory().isSegmentLinked() );
        
        assertTrue( rtn1Rs.isRuleLinked() );
        assertTrue( rtn2Rs.isRuleLinked() );
        assertFalse( rtn3Rs.isRuleLinked() );         
        
        // Link in Rule3
        n6.assertObject( f1, context, wm );
        n7.assertObject( f1, context, wm );
        assertTrue( bm.getSegmentMemory().isSegmentLinked() );
        
        assertTrue( rtn1Rs.isRuleLinked() );
        assertTrue( rtn2Rs.isRuleLinked() );
        assertTrue( rtn3Rs.isRuleLinked() );  
        
        // retract n2, should unlink all rules
        n2.retractRightTuple( f2.getFirstRightTuple(), context, wm );
        assertFalse( rtn1Rs.isRuleLinked() );
        assertFalse( rtn2Rs.isRuleLinked() );
        assertFalse( rtn3Rs.isRuleLinked() );        
    }
   
}
